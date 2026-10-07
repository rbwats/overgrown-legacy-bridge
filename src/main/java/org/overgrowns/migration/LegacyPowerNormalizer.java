package org.overgrowns.migration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.function.Predicate;

/** Context-aware legacy upgrades before Apoli expands multiple powers. */
public final class LegacyPowerNormalizer {
    private LegacyPowerNormalizer() {}

    /** Evaluates {@code fabric:load_conditions}; replaced with Fabric's evaluator at initialization. */
    public static volatile Predicate<JsonObject> loadConditions = json -> true;

    public static void normalizeAll(Map<ResourceLocation, JsonElement> data) {
        int changed = 0, skipped = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            JsonElement original = entry.getValue();
            if (original == null || !original.isJsonObject()) continue;
            try {
                JsonElement copy = original.deepCopy();
                boolean updated = LegacyBuiltinPowers.adapt(entry.getKey(), copy.getAsJsonObject());
                updated |= filterSubPowers(entry.getKey(), copy.getAsJsonObject());
                updated |= normalize(copy);
                JsonElement repaired = repairSpecific(entry.getKey(), copy);
                if (repaired != copy) updated = true;
                if (updated) {
                    entry.setValue(repaired);
                    changed++;
                }
            } catch (RuntimeException error) {
                // A malformed file must only affect itself, as it did on legacy Apoli.
                skipped++;
                LegacyBridge.LOGGER.error("Legacy normalization failed for power {}; loading it unchanged: {}", entry.getKey(), error.toString());
            }
        }
        try {
            LegacyCompatibilityReport.write(data, changed);
        } catch (RuntimeException error) {
            LegacyBridge.LOGGER.warn("Could not build the legacy compatibility report", error);
        }
        LegacyBridge.LOGGER.info("Legacy power JSON normalization changed {} file(s); {} could not be normalized", changed, skipped);
    }

    /** Legacy Apoli evaluated fabric:load_conditions on every sub-power of a multiple power. */
    static boolean filterSubPowers(ResourceLocation id, JsonObject root) {
        String type = string(root, "type");
        if (type == null || !type.endsWith(":multiple")) return false;
        // The top-level list belongs to Fabric's own resource-condition filtering and is left in place.
        boolean changed = false;
        for (Map.Entry<String, JsonElement> field : new java.util.ArrayList<>(root.entrySet())) {
            if (!field.getValue().isJsonObject()) continue;
            JsonObject sub = field.getValue().getAsJsonObject();
            if (!sub.has(LOAD_CONDITIONS_KEY)) continue;
            boolean keep;
            try {
                keep = loadConditions.test(sub);
            } catch (RuntimeException error) {
                LegacyBridge.LOGGER.error("Could not evaluate the load conditions of sub-power {}_{} (skipping): {}", id, field.getKey(), error.toString());
                keep = false;
            }
            if (keep) sub.remove(LOAD_CONDITIONS_KEY); else root.remove(field.getKey());
            changed = true;
        }
        return changed;
    }

    static final String LOAD_CONDITIONS_KEY = "fabric:load_conditions";

    static boolean normalize(JsonElement node) {
        return LegacySchema.normalize(node, LegacySchema.Context.POWER);
    }

    static boolean normalizeNode(JsonObject obj, LegacySchema.Context context) {
        boolean changed = false;
        // The old Origins HUD atlases moved in Overgrown's Origins. Rewrite only
        // known built-in locations so custom resource-pack sprites remain intact.
        String sprite = string(obj, "sprite_location");
        if (sprite != null) {
            String updated = legacyHudSprite(sprite);
            if (!sprite.equals(updated)) {
                obj.addProperty("sprite_location", updated);
                changed = true;
            }
        }

        String type = string(obj, "type");
        if (type == null) return changed;
        String path = type.substring(type.indexOf(':') + 1);
        if (context != LegacySchema.Context.POWER && java.util.Set.of(
            "active_self", "ignore_water", "invulnerability", "prevent_block_use", "modify_movement_speed",
            "sprint_jumping", "launch", "fire_power", "no_shield", "edible_item", "custom_death_sound", "custom_hurt_sound").contains(path)) return changed;
        if (path.equals("damage") && context != LegacySchema.Context.ENTITY_ACTION && context != LegacySchema.Context.BI_ENTITY_ACTION) return changed;
        if (path.equals("entity_group") && context != LegacySchema.Context.ENTITY_CONDITION) return changed;
        if (path.equals("attacker") && context != LegacySchema.Context.DAMAGE_CONDITION) return changed;
        switch (type) {
            case "origins:active_self", "apoli:active_self" -> {
                JsonObject condition = obj.has("condition") && obj.get("condition").isJsonObject()
                    ? obj.getAsJsonObject("condition") : null;
                if (condition != null
                    && ("origins:consumed_item".equals(string(condition, "type"))
                        || "apoli:consumed_item".equals(string(condition, "type")))
                    && condition.has("item_condition")
                    && !obj.has("key") && !obj.has("cooldown") && !obj.has("hud_render")) {
                    obj.addProperty("type", "apoli:action_on_item_use");
                    obj.addProperty("trigger", "finish");
                    obj.add("item_condition", condition.get("item_condition").deepCopy());
                    obj.remove("condition");
                    changed = true;
                }
            }
            case "origins:add_velocity", "apoli:add_velocity" -> {
                String space = string(obj, "space");
                if ("velocity_normalized_to_actor".equals(space)
                    || "velocity_normalized_to_target".equals(space)) {
                    obj.addProperty("type", "overgrown_legacy_bridge:add_velocity_relative");
                    obj.addProperty("relative_to", space.endsWith("target") ? "target" : "actor");
                    obj.remove("space");
                    changed = true;
                }
            }
            case "origins:ignore_water", "apoli:ignore_water" -> {
                if (!obj.has("fluid_condition")) {
                    JsonObject condition = typed("apoli:in_tag");
                    condition.addProperty("tag", "minecraft:water");
                    obj.add("fluid_condition", condition);
                    changed = true;
                }
            }
            case "origins:invulnerability", "apoli:invulnerability" -> {
                if (!obj.has("damage_condition")) {
                    obj.add("damage_condition", always());
                    changed = true;
                }
            }
            case "origins:prevent_block_use", "apoli:prevent_block_use" -> {
                if (!obj.has("block_condition")) {
                    obj.add("block_condition", always());
                    changed = true;
                }
            }
            case "origins:in_block", "apoli:in_block" -> {
                if (!obj.has("block_condition") && obj.has("block")) {
                    JsonObject condition = typed("apoli:block");
                    condition.add("block", obj.remove("block"));
                    obj.add("block_condition", condition);
                    changed = true;
                }
            }
            case "origins:grant_power", "apoli:grant_power",
                 "origins:revoke_power", "apoli:revoke_power" -> {
                if (!obj.has("source")) {
                    obj.addProperty("source", "overgrown_legacy_bridge:legacy_action");
                    changed = true;
                }
            }
            case "origins:damage", "apoli:damage" -> {
                if (!obj.has("damage_type")) {
                    obj.addProperty("damage_type", legacyDamageType(obj));
                    changed = true;
                }
            }
            case "origins:and", "apoli:and" -> {
                if (context.action() && obj.has("actions") && obj.get("actions").isJsonObject()) {
                    JsonArray actions = new JsonArray();
                    actions.add(obj.get("actions").deepCopy());
                    obj.add("actions", actions);
                    changed = true;
                }
                if (context.condition() && !obj.has("conditions") && !obj.has("actions")) {
                    obj.addProperty("type", "apoli:constant");
                    obj.addProperty("value", true);
                    changed = true;
                }
            }
            case "origins:attacker", "apoli:attacker" -> {
                JsonObject distance = singleDistance(obj.get("entity_condition"));
                if (distance != null && distance.has("comparison") && distance.has("compare_to")) {
                    obj.addProperty("type", "overgrown_legacy_bridge:attacker_distance");
                    obj.add("comparison", distance.get("comparison").deepCopy());
                    obj.add("compare_to", distance.get("compare_to").deepCopy());
                    if (distance.has("inverted"))
                        obj.add("inverted_distance", distance.get("inverted").deepCopy());
                    obj.remove("entity_condition");
                    changed = true;
                }
            }
            case "origins:attacker_action", "apoli:attacker_action" -> {
                JsonObject action = obj.has("entity_action") && obj.get("entity_action").isJsonObject()
                    ? obj.getAsJsonObject("entity_action") : null;
                JsonObject condition = obj.has("condition") && obj.get("condition").isJsonObject()
                    ? obj.getAsJsonObject("condition") : null;
                if (action != null && condition != null && action.has("actions")
                    && action.get("actions").isJsonArray()
                    && action.getAsJsonArray("actions").isEmpty()) {
                    JsonObject replacement = condition.deepCopy();
                    obj.remove("entity_action");
                    obj.remove("condition");
                    for (Map.Entry<String, JsonElement> field : replacement.entrySet())
                        obj.add(field.getKey(), field.getValue().deepCopy());
                    changed = true;
                }
            }
            case "origins:entity_group", "apoli:entity_group" -> {
                if ("minecraft:passive".equals(string(obj, "group"))) {
                    obj.addProperty("type", "overgrown_legacy_bridge:passive_mob");
                    obj.remove("group");
                    changed = true;
                }
            }
            case "apugli:edible_item" -> {
                obj.addProperty("type", "apoli:edible_item");
                changed = true;
            }
            case "origins:modify_movement_speed", "apoli:modify_movement_speed" -> {
                JsonElement mod = obj.get("modifier");
                if (mod != null && mod.isJsonObject()) {
                    mod.getAsJsonObject().addProperty("attribute", "minecraft:generic.movement_speed");
                    obj.addProperty("type", "apoli:attribute");
                    changed = true;
                }
            }
            case "origins:no_shield", "apoli:no_shield" -> {
                obj.addProperty("type", "apoli:prevent_item_use");
                JsonObject itemCondition = typed("apoli:ingredient");
                JsonObject ingredient = new JsonObject();
                ingredient.addProperty("item", "minecraft:shield");
                itemCondition.add("ingredient", ingredient);
                obj.add("item_condition", itemCondition);
                changed = true;
            }
            case "origins:sprint_jumping", "apoli:sprint_jumping" -> {
                JsonElement speed = obj.remove("speed");
                if (speed != null) {
                    JsonObject modifier = new JsonObject();
                    modifier.addProperty("operation", "addition");
                    modifier.add("value", speed);
                    obj.addProperty("type", "apoli:modify_jump");
                    obj.add("modifier", modifier);
                    JsonElement prior = obj.get("condition");
                    if (prior == null) obj.add("condition", typed("apoli:sprinting"));
                    else {
                        JsonArray conditions = new JsonArray();
                        conditions.add(prior.deepCopy());
                        conditions.add(typed("apoli:sprinting"));
                        JsonObject combined = typed("apoli:and");
                        combined.add("conditions", conditions);
                        obj.add("condition", combined);
                    }
                    changed = true;
                }
            }
            case "origins:launch", "apoli:launch" -> {
                JsonElement speed = obj.remove("speed");
                if (speed != null) {
                    JsonObject velocity = typed("apoli:add_velocity");
                    velocity.add("y", speed);
                    JsonObject action = velocity;
                    String sound = string(obj, "sound");
                    if (sound != null) {
                        JsonObject play = typed("apoli:play_sound");
                        play.addProperty("sound", sound);
                        JsonArray actions = new JsonArray();
                        actions.add(velocity);
                        actions.add(play);
                        action = typed("apoli:and");
                        action.add("actions", actions);
                        obj.remove("sound");
                    }
                    obj.addProperty("type", "apoli:action_on_key_press");
                    obj.add("entity_action", action);
                    changed = true;
                }
            }
            case "origins:fire_power", "apoli:fire_power" -> {
                obj.addProperty("type", "apoli:fire_projectile");
                String entity = string(obj, "entity_type");
                if (entity != null && entity.startsWith("minecraft_") && entity.indexOf(':') < 0)
                    obj.addProperty("entity_type", "minecraft:" + entity.substring("minecraft_".length()));
                String tag = string(obj, "tag");
                if (tag != null && tag.contains("},{")) obj.addProperty("tag", tag.replace("},{", ","));
                String sound = string(obj, "sound");
                if ("beacon.activate".equals(sound)) obj.addProperty("sound", "minecraft:block.beacon.activate");
                if (obj.has("key") && obj.get("key").isJsonObject()) {
                    JsonObject key = obj.getAsJsonObject("key");
                    if (key.has("continous")) key.add("continuous", key.remove("continous"));
                    if ("origins.primary_active".equals(string(key, "key")))
                        key.addProperty("key", "key.origins.primary_active");
                }
                changed = true;
            }
            case "apugli:custom_death_sound", "apugli:custom_hurt_sound" -> {
                String sound = string(obj, "sound");
                if (sound != null) {
                    JsonObject replacement = new JsonObject();
                    replacement.addProperty("id", sound);
                    if (obj.has("pitch")) replacement.add("pitch", obj.remove("pitch"));
                    JsonObject sounds = new JsonObject();
                    String original = type.endsWith("death_sound")
                        ? "minecraft:entity.player.death" : "minecraft:entity.player.hurt";
                    sounds.add(original, replacement);
                    obj.add("sounds", sounds);
                    obj.remove("sound");
                    obj.addProperty("type", "apoli:replace_sound_emission");
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static String legacyHudSprite(String sprite) {
        if (sprite.equals("origins:textures/gui/resource_bar.png"))
            return "apoli:textures/gui/resource_bar.png";
        String oldPrefix = "origins:textures/gui/community/";
        if (sprite.startsWith(oldPrefix))
            return "origins:textures/gui/sprites/hud_render/" + sprite.substring(oldPrefix.length());
        return sprite;
    }

    private static JsonObject singleDistance(JsonElement candidate) {
        if (candidate == null || !candidate.isJsonObject()) return null;
        JsonObject value = candidate.getAsJsonObject();
        String type = string(value, "type");
        if ("origins:distance".equals(type) || "apoli:distance".equals(type)) return value;
        if (!("origins:and".equals(type) || "apoli:and".equals(type))
            || !value.has("conditions") || !value.get("conditions").isJsonArray()) return null;
        JsonArray conditions = value.getAsJsonArray("conditions");
        return conditions.size() == 1 ? singleDistance(conditions.get(0)) : null;
    }

    private static JsonElement repairSpecific(ResourceLocation id, JsonElement input) {
        if (!input.isJsonObject()) return input;
        JsonObject root = input.getAsJsonObject();
        if (id.toString().equals("dragon_origins:mimic/chest")
            && "origins:and".equals(string(root, "type"))
            && root.has("actions") && root.get("actions").isJsonArray()) {
            JsonArray actions = root.getAsJsonArray("actions");
            if (actions.size() != 2 || !actions.get(0).isJsonObject()
                || !actions.get(1).isJsonObject()) return input;
            JsonObject replacement = typed("apoli:multiple");
            if (root.has("hidden")) replacement.add("hidden", root.get("hidden").deepCopy());
            if (root.has("name")) replacement.add("name", root.get("name").deepCopy());
            if (root.has("description")) replacement.add("description", root.get("description").deepCopy());
            JsonObject invisibility = actions.get(0).getAsJsonObject().deepCopy();
            JsonObject attribute = actions.get(1).getAsJsonObject().deepCopy();
            if (root.has("condition")) {
                invisibility.add("condition", root.get("condition").deepCopy());
                attribute.add("condition", root.get("condition").deepCopy());
            }
            replacement.add("legacy_invisibility", invisibility);
            replacement.add("legacy_attribute", attribute);
            return replacement;
        }
        if (id.toString().equals("dragon_origins:mimic/chest_off") && !root.has("type")
            && root.has("toggle") && root.get("toggle").isJsonObject()
            && root.has("action_off") && root.get("action_off").isJsonObject()) {
            JsonObject off = root.getAsJsonObject("action_off");
            if (!off.has("actions") || !off.get("actions").isJsonArray()
                || off.getAsJsonArray("actions").size() != 1
                || !off.getAsJsonArray("actions").get(0).isJsonObject()) return input;
            JsonObject replacement = typed("apoli:multiple");
            if (root.has("hidden")) replacement.add("hidden", root.get("hidden").deepCopy());
            if (root.has("description")) replacement.add("description", root.get("description").deepCopy());
            replacement.add("toggle", root.get("toggle").deepCopy());
            JsonObject attribute = off.getAsJsonArray("actions").get(0).getAsJsonObject().deepCopy();
            if (off.has("condition")) attribute.add("condition", off.get("condition").deepCopy());
            replacement.add("off_attribute", attribute);
            return replacement;
        }
        return input;
    }

    private static String legacyDamageType(JsonObject action) {
        JsonElement source = action.get("source");
        if (source != null && source.isJsonObject()) {
            String name = string(source.getAsJsonObject(), "name");
            if (name != null) {
                if (name.contains("freeze")) return "minecraft:freeze";
                if (name.contains("magic") || name.contains("extinguish")) return "minecraft:magic";
            }
        }
        return "minecraft:generic";
    }

    private static String string(JsonObject obj, String key) {
        JsonElement value = obj.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
            ? value.getAsString() : null;
    }

    private static JsonObject typed(String id) {
        JsonObject value = new JsonObject();
        value.addProperty("type", id);
        return value;
    }

    private static JsonObject always() {
        JsonObject value = typed("apoli:constant");
        value.addProperty("value", true);
        return value;
    }
}
