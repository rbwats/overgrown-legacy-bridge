package org.overgrowns.migration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.RestrictArmorPower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Legacy conditioned_attribute and conditioned_restrict_armor re-checked their condition every tick_rate ticks.
 * Overgrown's codecs drop the field, so the rate is recorded per loaded power id while powers are parsed.
 */
public final class LegacyTickRates {
    private LegacyTickRates() {}

    private static final Map<ResourceLocation, Integer> ATTRIBUTE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Integer> ARMOR = new ConcurrentHashMap<>();
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    /** Overgrown already sweeps restricted armor this often; only faster legacy rates need an extra sweep. */
    private static final int OVERGROWN_ARMOR_SWEEP = 20;

    public static void clear() {
        ATTRIBUTE.clear();
        ARMOR.clear();
    }

    /** Records the legacy rate of one power as Overgrown is about to parse it; sub-powers arrive with their own ids. */
    public static void record(ResourceLocation id, JsonElement json) {
        if (json == null || !json.isJsonObject()) return;
        JsonObject power = json.getAsJsonObject();
        String type = LegacySchema.string(power, "type");
        if (type == null) return;
        switch (type) {
            case "origins:conditioned_attribute", "apoli:conditioned_attribute" -> ATTRIBUTE.put(id, rate(power, 20));
            case "origins:conditioned_restrict_armor", "apoli:conditioned_restrict_armor" -> ARMOR.put(id, rate(power, 80));
            default -> { }
        }
    }

    static int rate(JsonObject power, int fallback) {
        JsonElement value = power.get("tick_rate");
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) return fallback;
        // Legacy age % 0 threw every tick; treat non-positive rates as every tick instead.
        return Math.max(1, value.getAsInt());
    }

    /** Whether Overgrown's attribute tick should re-evaluate this power now. */
    public static boolean attributeTicks(ResourceLocation powerId, LivingEntity owner) {
        Integer rate = ATTRIBUTE.get(powerId);
        return rate == null || rate <= 1 || owner == null || owner.tickCount % rate == 0;
    }

    public static void sweepArmor(MinecraftServer server) {
        if (ARMOR.isEmpty()) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PowerLookup.forEachEntry(player, ApoliIds.RESTRICT_ARMOR, RestrictArmorPower.Config.class, (powerId, cfg) -> {
                Integer rate = ARMOR.get(powerId);
                if (rate == null || rate >= OVERGROWN_ARMOR_SWEEP || player.tickCount % rate != 0) return;
                for (EquipmentSlot slot : ARMOR_SLOTS) {
                    ItemStack stack = player.getItemBySlot(slot);
                    if (stack.isEmpty() || !RestrictArmorPower.blocks(cfg, slot, new ItemCtx(stack, player.level(), player))) continue;
                    player.setItemSlot(slot, ItemStack.EMPTY);
                    if (!player.getInventory().add(stack) && !stack.isEmpty()) player.drop(stack, false);
                }
            });
        }
    }
}
