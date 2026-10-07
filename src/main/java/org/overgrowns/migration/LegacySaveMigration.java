package org.overgrowns.migration;

import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerResources;
import dev.overgrown.apoli.power.PowerSources;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.builtin.InventoryPower;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginRegistry;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Moves Origins 1.10.0 / Apoli 2.9.0 player data (Cardinal Components "origins:origin" and "apoli:powers")
 * onto Overgrown on the first join: chosen origins before Overgrown's join logic would prompt a new choice,
 * then power state once Overgrown has re-granted the origin powers. Until then the legacy tag is saved back.
 */
public final class LegacySaveMigration {
    private LegacySaveMigration() {}

    public static final String COMPONENTS = "cardinal_components";
    static final String ORIGIN_COMPONENT = "origins:origin";
    static final String POWER_COMPONENT = "apoli:powers";
    private static final ResourceLocation EMPTY_ORIGIN = new ResourceLocation("origins", "empty");
    private static final ResourceLocation TOGGLE = new ResourceLocation("apoli", "toggle");
    private static final ResourceLocation INVENTORY = new ResourceLocation("apoli", "inventory");

    /** Implemented by ServerPlayer: the legacy components read from its save, until migrated. */
    public interface Holder {
        CompoundTag overgrownLegacyBridge$legacyComponents();
        void overgrownLegacyBridge$setLegacyComponents(CompoundTag components);
    }

    /** Only legacy components are kept; anything else under the key belongs to whichever mod still reads it. */
    public static CompoundTag capture(CompoundTag save) {
        if (!save.contains(COMPONENTS, Tag.TAG_COMPOUND)) return null;
        CompoundTag components = save.getCompound(COMPONENTS), legacy = new CompoundTag();
        for (String key : new String[]{ORIGIN_COMPONENT, POWER_COMPONENT})
            if (components.contains(key, Tag.TAG_COMPOUND)) legacy.put(key, components.getCompound(key).copy());
        return legacy.isEmpty() ? null : legacy;
    }

    public static void writeBack(CompoundTag save, CompoundTag legacy) {
        CompoundTag components = save.getCompound(COMPONENTS);
        for (String key : legacy.getAllKeys()) if (!components.contains(key)) components.put(key, legacy.get(key).copy());
        save.put(COMPONENTS, components);
    }

    /** Layer to origin, in either the layered format or the single "Origin" key of older saves. */
    public static Map<ResourceLocation, ResourceLocation> origins(CompoundTag legacy) {
        Map<ResourceLocation, ResourceLocation> result = new LinkedHashMap<>();
        CompoundTag component = legacy.getCompound(ORIGIN_COMPONENT);
        if (component.contains("Origin", Tag.TAG_STRING)) {
            ResourceLocation origin = ResourceLocation.tryParse(component.getString("Origin"));
            if (origin != null) result.put(new ResourceLocation("origins", "origin"), origin);
            return result;
        }
        ListTag layers = component.getList("OriginLayers", Tag.TAG_COMPOUND);
        for (int i = 0; i < layers.size(); i++) {
            ResourceLocation layer = ResourceLocation.tryParse(layers.getCompound(i).getString("Layer"));
            ResourceLocation origin = ResourceLocation.tryParse(layers.getCompound(i).getString("Origin"));
            if (layer != null && origin != null && !origin.equals(EMPTY_ORIGIN)) result.put(layer, origin);
        }
        return result;
    }

    /** Runs before Overgrown's join handler, which would otherwise ask for every layer again. */
    public static void restoreOrigins(ServerPlayer player) {
        if (!(player instanceof Holder holder) || holder.overgrownLegacyBridge$legacyComponents() == null) return;
        CompoundTag legacy = holder.overgrownLegacyBridge$legacyComponents();
        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);
        Map<ResourceLocation, ResourceLocation> origins = origins(legacy);
        if (state.snapshot().isEmpty()) {
            for (Map.Entry<ResourceLocation, ResourceLocation> entry : origins.entrySet()) {
                if (OriginLayers.get(entry.getKey()) == null) {
                    LegacyBridge.LOGGER.warn("Could not find origin layer {}, which existed on the legacy data of {}", entry.getKey(), player.getName().getString());
                    continue;
                }
                if (OriginRegistry.get(entry.getValue()) == null) {
                    LegacyBridge.LOGGER.warn("Could not find origin {}, which existed on the legacy data of {}", entry.getValue(), player.getName().getString());
                    continue;
                }
                state.setOrigin(entry.getKey(), entry.getValue());
                state.setPinned(entry.getKey(), true);
            }
            if (!origins.isEmpty() || legacy.getCompound(ORIGIN_COMPONENT).getBoolean("HadOriginBefore")) state.setFirstJoinDone(true);
            LegacyBridge.LOGGER.info("Migrated legacy origins of {}: {}", player.getName().getString(), state.snapshot());
        }
        regrantPowers(player, legacy, Set.copyOf(origins.values()));
    }

    /** Powers granted by anything other than an origin (commands, grant_power actions) keep their source. */
    static void regrantPowers(ServerPlayer player, CompoundTag legacy, Set<ResourceLocation> originSources) {
        PowerContainer container = PowerContainerAttachment.getOrCreate(player);
        if (container == null) return;
        ListTag powers = legacy.getCompound(POWER_COMPONENT).getList("Powers", Tag.TAG_COMPOUND);
        Map<ResourceLocation, Set<ResourceLocation>> grants = new LinkedHashMap<>();
        for (int i = 0; i < powers.size(); i++) {
            CompoundTag entry = powers.getCompound(i);
            ResourceLocation power = ResourceLocation.tryParse(entry.getString("Type"));
            if (power == null) continue;
            ListTag sources = entry.getList("Sources", Tag.TAG_STRING);
            for (int s = 0; s < sources.size(); s++) {
                ResourceLocation source = ResourceLocation.tryParse(sources.getString(s));
                if (source != null && !originSources.contains(source)) grants.computeIfAbsent(power, key -> new HashSet<>()).add(source);
            }
        }
        // Legacy listed sub-powers beside their multiple power; Overgrown grants those through the parent.
        Set<ResourceLocation> subPowers = new HashSet<>();
        for (ResourceLocation power : grants.keySet()) {
            Set<ResourceLocation> nested = PowerSources.powersOf(power);
            if (nested != null) subPowers.addAll(nested);
        }
        grants.forEach((power, sources) -> {
            if (subPowers.contains(power)) return;
            if (ApoliPowers.get(power) == null) {
                LegacyBridge.LOGGER.warn("Legacy power {} of {} is not loaded; not granting it", power, player.getName().getString());
                return;
            }
            for (ResourceLocation source : sources) container.addPower(power, source);
        });
    }

    /** Runs after Overgrown's join handler has granted the origin powers. */
    public static void restorePowerData(ServerPlayer player) {
        if (!(player instanceof Holder holder) || holder.overgrownLegacyBridge$legacyComponents() == null) return;
        CompoundTag legacy = holder.overgrownLegacyBridge$legacyComponents();
        if (PowerContainer.of(player) instanceof PowerContainerImpl container) {
            ListTag powers = legacy.getCompound(POWER_COMPONENT).getList("Powers", Tag.TAG_COMPOUND);
            int restored = 0;
            for (int i = 0; i < powers.size(); i++) {
                CompoundTag entry = powers.getCompound(i);
                ResourceLocation power = ResourceLocation.tryParse(entry.getString("Type"));
                if (power != null && container.hasPower(power) && restore(container, power, entry.get("Data"))) restored++;
            }
            if (restored > 0) InventoryPower.syncAll(player);
            LegacyBridge.LOGGER.info("Migrated legacy power data of {}: {} power(s) restored", player.getName().getString(), restored);
        }
        holder.overgrownLegacyBridge$setLegacyComponents(null);
    }

    /**
     * Resource values, toggle states and power inventories carry over. Cooldowns stored a world time the
     * new cooldowns do not use, so they start ready.
     */
    static boolean restore(PowerContainerImpl container, ResourceLocation power, Tag data) {
        Power loaded = ApoliPowers.get(power);
        if (loaded == null || data == null) return false;
        ResourceLocation type = PowerTypeRegistry.resolveId(loaded.typeId());
        if (data instanceof IntTag value) return PowerResources.write(container, power, value.getAsInt()).isPresent();
        if (data instanceof ByteTag value && (TOGGLE.equals(type) || LegacyToggleNightVisionPower.ID.equals(type))) {
            container.setAuxInt(power, value.getAsByte() > 0 ? 1 : 0);
            return true;
        }
        if (data instanceof CompoundTag value && INVENTORY.equals(type) && value.contains("Items", Tag.TAG_LIST)) {
            container.setAuxNbt(power, value.copy());
            return true;
        }
        return false;
    }
}
