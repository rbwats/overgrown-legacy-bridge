package org.overgrowns.migration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Legacy attribute_modify_transfer: an attribute's current modifiers, scaled by a multiplier, join the value a
 * power class modifies. Legacy Apoli merged them with the class's own power modifiers in one pass; here they
 * apply to the value after Overgrown's handler, which is identical when the holder has no power of that class.
 */
public final class LegacyAttributeTransferPower extends PowerType<LegacyAttributeTransferPower.Config> {
    public static final ResourceLocation ID = new ResourceLocation(LegacyBridge.MOD_ID, "attribute_modify_transfer");

    /** Legacy power classes (snake_case without "Power") that PowerHolderComponent.modify was called with. */
    public static final Set<String> SUPPORTED = Set.of(
        "modify_air_speed", "modify_velocity", "modify_jump", "modify_slipperiness", "modify_healing", "modify_falling",
        "modify_exhaustion", "modify_break_speed", "modify_damage_dealt", "modify_damage_taken", "modify_projectile_damage",
        "modify_status_effect_amplifier", "modify_status_effect_duration", "modify_experience", "modify_insomnia_ticks",
        "modify_attribute");

    /**
     * Every class in Apoli 2.9.0's power package. Calio accepted any of them; a transfer to a class that
     * PowerHolderComponent.modify never received (not in SUPPORTED) loaded and did nothing.
     */
    private static final Set<String> LEGACY_CLASSES = Set.of(
        "action_on_being_used", "action_on_block_break", "action_on_block_use", "action_on_callback",
        "action_on_entity_use", "action_on_hit", "action_on_item_use", "action_on_land", "action_over_time",
        "action_when_hit", "active_cooldown", "active_interaction", "attacker_action_when_hit", "attribute",
        "attribute_modify_transfer", "burn", "climbing", "conditioned_attribute", "conditioned_restrict_armor",
        "cooldown", "damage_over_time", "disable_regen", "effect_immunity", "elytra_flight", "entity_glow",
        "exhaust_over_time", "fire_immunity", "fire_projectile", "float", "freeze", "grounded",
        "hud_rendered_variable_int", "ignore_water", "interaction", "inventory", "invisibility", "invulnerable",
        "item_on_item", "keep_inventory", "lava_vision", "model_color", "modify_air_speed", "modify_attribute",
        "modify_block_render", "modify_break_speed", "modify_camera_submersion_type", "modify_crafting",
        "modify_damage_dealt", "modify_damage_taken", "modify_exhaustion", "modify_experience", "modify_falling",
        "modify_fluid_render", "modify_food", "modify_grindstone", "modify_harvest", "modify_healing",
        "modify_insomnia_ticks", "modify_jump", "modify_lava_speed", "modify_player_spawn", "modify_projectile_damage",
        "modify_slipperiness", "modify_status_effect_amplifier", "modify_status_effect_duration", "modify_swim_speed",
        "modify_velocity", "night_vision", "overlay", "override_hud_texture", "particle", "phasing", "player_ability",
        "prevent_being_used", "prevent_block_selection", "prevent_block_use", "prevent_death", "prevent_elytra_flight",
        "prevent_entity_collision", "prevent_entity_render", "prevent_entity_use", "prevent_feature_render",
        "prevent_game_event", "prevent_item_use", "prevent_sleep", "prevent_sprinting", "recipe", "replace_loot_table",
        "resource", "restrict_armor", "self_action_on_hit", "self_action_on_kill", "self_action_when_hit", "self_glow",
        "set_entity_group", "shader", "shaking", "simple_status_effect", "stacking_status_effect",
        "starting_equipment", "status_effect", "swimming", "target_action_on_hit", "toggle", "toggle_night_vision",
        "tooltip", "value_modifying", "variable_int", "walk_on_fluid");

    /** Classes any loaded transfer power has named; lets hot paths skip the lookup. Never shrinks. */
    private static final Set<String> USED = ConcurrentHashMap.newKeySet();

    public record Config(String modifyClass, ResourceLocation attribute, double multiplier) {}

    private static final Codec<String> CLASS = Codec.STRING.flatXmap(name -> {
        String key = classKey(name);
        if (!LEGACY_CLASSES.contains(key))
            return DataResult.error(() -> "Specified class does not exist: \"" + name + "\"");
        USED.add(key);
        return DataResult.success(key);
    }, DataResult::success);

    /** Calio resolved a class by full name, simple name or snake_case name; all reduce to snake_case here. */
    public static String classKey(String name) {
        String key = name.trim();
        if (key.indexOf(':') >= 0) key = key.substring(key.indexOf(':') + 1);
        if (key.lastIndexOf('.') >= 0) key = key.substring(key.lastIndexOf('.') + 1);
        if (key.endsWith("Power")) key = key.substring(0, key.length() - "Power".length());
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (Character.isUpperCase(c) && i > 0 && key.charAt(i - 1) != '_') out.append('_');
            out.append(Character.toLowerCase(c));
        }
        return out.toString().toLowerCase(Locale.ROOT);
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            CLASS.fieldOf("class").forGetter(Config::modifyClass),
            IdCodecs.ID.fieldOf("attribute").forGetter(Config::attribute),
            Codec.DOUBLE.optionalFieldOf("multiplier", 1.0).forGetter(Config::multiplier)
        ).apply(i, Config::new));
    }

    public static boolean isUsed(String modifyClass) {
        return USED.contains(modifyClass);
    }

    /** Applies the holder's transfers for a legacy class to a value; non-living holders have no attributes. */
    public static double apply(Entity holder, String modifyClass, double value) {
        if (!(holder instanceof LivingEntity entity) || !USED.contains(modifyClass)) return value;
        double[] result = {value};
        PowerLookup.forEach(entity, ID, Config.class, cfg -> {
            if (!cfg.modifyClass().equals(modifyClass)) return;
            Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(cfg.attribute());
            AttributeInstance instance = attribute == null ? null : entity.getAttribute(attribute);
            if (instance != null) result[0] = transfer(result[0], instance, cfg.multiplier());
        });
        return Double.isFinite(result[0]) ? result[0] : value;
    }

    public static float apply(Entity holder, String modifyClass, float value) {
        return (float) apply(holder, modifyClass, (double) value);
    }

    public static float modifyAirSpeed(LivingEntity entity, float value) {
        return apply(entity, "modify_air_speed", value);
    }

    /** Vanilla attribute order: additions, then base multipliers, then total multipliers. */
    private static double transfer(double base, AttributeInstance instance, double multiplier) {
        double value = base;
        for (AttributeModifier mod : instance.getModifiers())
            if (mod.getOperation() == AttributeModifier.Operation.ADDITION) value += mod.getAmount() * multiplier;
        double added = value;
        for (AttributeModifier mod : instance.getModifiers())
            if (mod.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE) value += added * mod.getAmount() * multiplier;
        for (AttributeModifier mod : instance.getModifiers())
            if (mod.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL) value *= 1.0 + mod.getAmount() * multiplier;
        return value;
    }
}
