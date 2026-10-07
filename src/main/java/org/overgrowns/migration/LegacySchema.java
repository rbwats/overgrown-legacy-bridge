package org.overgrowns.migration;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Traverse only schema fields, keeping recipes, NBT, text and third-party payloads intact. */
public final class LegacySchema {
    public enum Context {
        POWER, ENTITY_ACTION, BI_ENTITY_ACTION, BLOCK_ACTION, ITEM_ACTION,
        ENTITY_CONDITION, BI_ENTITY_CONDITION, BLOCK_CONDITION, ITEM_CONDITION,
        DAMAGE_CONDITION, FLUID_CONDITION, BIOME_CONDITION;
        boolean action() { return name().endsWith("_ACTION"); }
        boolean condition() { return name().endsWith("_CONDITION"); }
        Context conditionContext() {
            return action() ? valueOf(name().replace("_ACTION", "_CONDITION")) : this;
        }
    }
    private static final JsonObject SCHEMAS = load();
    private LegacySchema() {}
    private static JsonObject load() {
        try (var stream = LegacySchema.class.getResourceAsStream("/legacy-1.20.1-schemas.json")) {
            if (stream == null) throw new IllegalStateException("Missing legacy schema inventory");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) { throw new IllegalStateException(e); }
    }
    public static JsonObject schemas() { return SCHEMAS.deepCopy(); }
    public static boolean normalize(JsonElement node, Context context) {
        if (node == null || node.isJsonNull()) return false;
        if (node.isJsonArray()) {
            boolean changed = false;
            for (JsonElement child : node.getAsJsonArray()) changed |= normalize(child, context);
            return changed;
        }
        if (!node.isJsonObject()) return false;
        JsonObject obj = node.getAsJsonObject();
        String type = string(obj, "type");
        boolean outerLegacyMath = LEGACY_MATH.get();
        // Modifiers of origins:-typed powers use the legacy modifier engine; apoli: is shared with Overgrown packs.
        if (context == Context.POWER && type != null && type.startsWith("origins:")) LEGACY_MATH.set(true);
        try {
            return normalizeObject(obj, context, type);
        } finally {
            LEGACY_MATH.set(outerLegacyMath);
        }
    }
    private static boolean normalizeObject(JsonObject obj, Context context, String type) {
        // Types redirected to the bridge namespace keep the legacy enum handling of their original spelling.
        String originalType = type;
        boolean changed = adapt(obj, context, type);
        changed |= LegacyPowerNormalizer.normalizeNode(obj, context);
        type = string(obj, "type");
        String path = type == null ? "" : type.substring(type.indexOf(':') + 1);
        JsonObject fields = fields(context, path);
        boolean multiple = context == Context.POWER && path.equals("multiple");
        boolean legacyType = (type != null && (type.startsWith("origins:") || type.startsWith("apoli:")))
            || (originalType != null && (originalType.startsWith("origins:") || originalType.startsWith("apoli:")));
        for (var entry : new ArrayList<>(obj.entrySet())) {
            String key = entry.getKey();
            JsonElement child = entry.getValue();
            if (legacyType && isEnumField(fields, key)) changed |= lowercaseEnum(obj, key, child);
            if (key.equals("hud_render") && child.isJsonObject()) {
                JsonObject hud = child.getAsJsonObject();
                changed |= LegacyPowerNormalizer.normalizeNode(hud, Context.ENTITY_CONDITION);
                changed |= normalize(hud.get("condition"), Context.ENTITY_CONDITION);
                continue;
            }
            if (multiple && child.isJsonObject() && child.getAsJsonObject().has("type")
                && !Set.of("condition", "load_condition", "name", "description", "hud_render", "skill").contains(key)) {
                changed |= normalize(child, Context.POWER);
                continue;
            }
            Context nested = fieldContext(fields, key, context, path);
            if (nested != null) changed |= normalize(child, nested);
            else if (Set.of("modifier", "modifiers", "food_modifier", "food_modifiers", "saturation_modifier",
                    "saturation_modifiers", "xp_modifier").contains(key)) changed |= normalizeModifiers(child);
        }
        return changed;
    }
    /** Calio enum types: names matched in either case. Overgrown's codecs take only the lowercase names. */
    private static final List<String> ENUM_TYPES = List.of("enumValue(", "ACTION_RESULT", "HAND", "AXIS_SET", "DIRECTION_SET",
        "EQUIPMENT_SLOT", "CAMERA_SUBMERSION_TYPE", "SPACE", "INVENTORY_TYPE", "PROCESS_MODE", "RESOURCE_OPERATION");
    private static boolean isEnumField(JsonObject fields, String key) {
        if (fields == null || !fields.has(key) || !fields.get(key).isJsonObject()) return false;
        String javaType = string(fields.getAsJsonObject(key), "java_type");
        if (javaType == null) return false;
        for (String marker : ENUM_TYPES) if (javaType.contains(marker)) return true;
        return false;
    }
    private static boolean lowercaseEnum(JsonObject obj, String key, JsonElement value) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String lower = value.getAsString().toLowerCase(Locale.ROOT);
            if (lower.equals(value.getAsString())) return false;
            obj.addProperty(key, lower);
            return true;
        }
        if (!value.isJsonArray()) return false;
        JsonArray array = value.getAsJsonArray();
        boolean changed = false;
        for (int i = 0; i < array.size(); i++) {
            JsonElement item = array.get(i);
            if (item.isJsonPrimitive() && item.getAsJsonPrimitive().isString() && !item.getAsString().equals(item.getAsString().toLowerCase(Locale.ROOT))) {
                array.set(i, new JsonPrimitive(item.getAsString().toLowerCase(Locale.ROOT)));
                changed = true;
            }
        }
        return changed;
    }
    @FunctionalInterface public interface Visitor { void visit(Context context, JsonObject node, String path); }
    public static void walk(JsonElement node, Context context, String location, Visitor visitor) {
        if (node == null || node.isJsonNull()) return;
        if (node.isJsonArray()) {
            int index = 0; for (JsonElement child : node.getAsJsonArray()) walk(child, context, location + "[" + index++ + "]", visitor);
            return;
        }
        if (!node.isJsonObject()) return;
        JsonObject obj = node.getAsJsonObject(); String type = string(obj, "type");
        if (type != null) visitor.visit(context, obj, location);
        String path = type == null ? "" : type.substring(type.indexOf(':') + 1);
        JsonObject fields = fields(context, path);
        for (var entry : obj.entrySet()) {
            String key = entry.getKey(); JsonElement child = entry.getValue();
            if (key.equals("hud_render") && child.isJsonObject()) {
                walk(child.getAsJsonObject().get("condition"), Context.ENTITY_CONDITION, location + ".hud_render.condition", visitor); continue;
            }
            if (context == Context.POWER && path.equals("multiple") && child.isJsonObject() && child.getAsJsonObject().has("type")
                    && !Set.of("condition", "load_condition", "name", "description", "hud_render", "skill").contains(key)) {
                walk(child, Context.POWER, location + "." + key, visitor); continue;
            }
            Context next = fieldContext(fields, key, context, path);
            if (next != null) walk(child, next, location + "." + key, visitor);
        }
    }
    private static Context fieldContext(JsonObject fields, String key, Context context, String path) {
        if (fields != null && fields.has(key)) {
            String javaType = string(fields.getAsJsonObject(key), "java_type");
            if (javaType != null) {
                // BIENTITY contains ENTITY: choose the more specific context before substring matching.
                if (javaType.contains("BIENTITY_ACTION")) return Context.BI_ENTITY_ACTION;
                if (javaType.contains("BIENTITY_CONDITION")) return Context.BI_ENTITY_CONDITION;
                for (Context candidate : Context.values()) {
                    if (candidate == Context.POWER) continue;
                    String token = candidate.name().replace("BI_ENTITY", "BIENTITY");
                    if (javaType.contains(token)) return candidate;
                }
            }
        }
        if (key.equals("load_condition")) return null;
        if (key.equals("condition")) {
            if (context == Context.POWER) return Context.ENTITY_CONDITION;
            if (context == Context.ENTITY_CONDITION && path.equals("biome")) return Context.BIOME_CONDITION;
            if (context == Context.BI_ENTITY_CONDITION && Set.of("actor_condition", "target_condition", "either", "both").contains(path))
                return Context.ENTITY_CONDITION;
            return context.conditionContext();
        }
        if (key.equals("conditions")) return context == Context.POWER ? Context.ENTITY_CONDITION : context.condition() ? context : context.conditionContext();
        if (Set.of("action", "element", "if_action", "else_action", "fail_action", "success_action", "actions").contains(key)) {
            if (context == Context.BI_ENTITY_ACTION && Set.of("actor_action", "target_action").contains(path)) return Context.ENTITY_ACTION;
            if (context == Context.ENTITY_ACTION && path.equals("equipped_item_action")) return Context.ITEM_ACTION;
            return context.action() ? context : null;
        }
        if (key.contains("bientity_action")) return Context.BI_ENTITY_ACTION;
        if (key.contains("bientity_condition")) return Context.BI_ENTITY_CONDITION;
        if (key.contains("block_action")) return Context.BLOCK_ACTION;
        if (key.contains("block_condition")) return Context.BLOCK_CONDITION;
        if (key.contains("item_action")) return Context.ITEM_ACTION;
        if (key.contains("item_condition")) return Context.ITEM_CONDITION;
        if (key.equals("fluid_condition")) return Context.FLUID_CONDITION;
        if (key.equals("damage_condition")) return Context.DAMAGE_CONDITION;
        if (key.endsWith("_action") || key.startsWith("entity_action_")) return Context.ENTITY_ACTION;
        if (key.endsWith("_condition")) return Context.ENTITY_CONDITION;
        return null;
    }
    private static JsonObject fields(Context context, String path) {
        JsonObject group = SCHEMAS.getAsJsonObject(context.name());
        return group != null && group.has(path) ? group.getAsJsonObject(path).getAsJsonObject("fields") : null;
    }
    private static boolean adapt(JsonObject obj, Context ctx, String type) {
        JsonObject before = obj.deepCopy();
        if (ctx == Context.POWER) {
            // Power text and keys are legacy conventions regardless of which mod provides the power type.
            translate(obj, "name"); translate(obj, "description");
            if (obj.has("key")) normalizeKey(obj, "key");
        }
        if (type == null || !(type.startsWith("origins:") || type.startsWith("apoli:"))) return !before.equals(obj);
        String path = type.substring(type.indexOf(':') + 1);
        if (ctx == Context.ENTITY_CONDITION && path.equals("power_type") && obj.has("power_type")) {
            obj.addProperty("type", "apoli:power");
            obj.add("power", obj.remove("power_type"));
        }
        if (ctx == Context.BIOME_CONDITION && path.equals("category") && string(obj, "category") != null) {
            obj.addProperty("type", "apoli:in_tag");
            obj.addProperty("tag", "apoli:category/" + obj.remove("category").getAsString());
        }
        if (ctx == Context.BLOCK_CONDITION && path.equals("material")) {
            // Malformed values are left for the native parser to reject this one condition.
            List<String> materials = new ArrayList<>();
            boolean valid = true;
            if (obj.has("material")) {
                if (string(obj, "material") != null) materials.add(string(obj, "material")); else valid = false;
            }
            if (obj.has("materials") && obj.get("materials").isJsonArray()) {
                for (JsonElement material : obj.getAsJsonArray("materials")) {
                    if (material.isJsonPrimitive() && material.getAsJsonPrimitive().isString()) materials.add(material.getAsString());
                    else valid = false;
                }
            }
            if (valid) {
                JsonArray conditions = new JsonArray();
                for (String material : materials) conditions.add(materialCondition(material));
                obj.remove("material"); obj.remove("materials");
                obj.addProperty("type", "apoli:or"); obj.add("conditions", conditions);
            }
        }
        if (ctx == Context.ENTITY_CONDITION && path.equals("elytra_flight_possible")) rename(obj, "check_ability", "check_abilities");
        if (ctx == Context.ITEM_CONDITION && path.equals("enchantment")) {
            if (!obj.has("comparison")) obj.addProperty("comparison", ">");
            if (!obj.has("compare_to")) obj.addProperty("compare_to", 0);
        }
        if (ctx == Context.ENTITY_ACTION && path.equals("apply_effect")) {
            JsonArray effects = new JsonArray();
            if (obj.has("effect")) append(effects, obj.remove("effect"));
            if (obj.has("effects")) append(effects, obj.remove("effects"));
            obj.add("effect", effects);
        }
        if ((ctx == Context.ENTITY_ACTION || ctx == Context.BI_ENTITY_ACTION) && path.equals("damage") && obj.has("source"))
            obj.addProperty("type", "overgrown_legacy_bridge:damage");
        if (ctx == Context.POWER) {
            if (path.equals("damage_over_time")) obj.addProperty("type", "overgrown_legacy_bridge:damage_over_time");
            if (path.equals("toggle_night_vision")) obj.addProperty("type", "overgrown_legacy_bridge:toggle_night_vision");
            // Overgrown registers modify_grindstone without applying it anywhere.
            if (path.equals("modify_grindstone")) obj.addProperty("type", "overgrown_legacy_bridge:modify_grindstone");
            if (path.equals("action_on_land") && !obj.has("entity_action")) {
                JsonObject noAction = new JsonObject(); noAction.addProperty("type", "apoli:nothing"); obj.add("entity_action", noAction);
            }
            if (path.equals("modify_camera_submersion") && !obj.has("from")) obj.addProperty("type", "overgrown_legacy_bridge:modify_camera_submersion");
            if (path.equals("prevent_sleep") && obj.has("message") && obj.get("message").isJsonPrimitive()) {
                JsonObject text = new JsonObject(); text.add("translate", obj.get("message")); obj.add("message", text);
            }
            if (path.equals("particle")) {
                if (!obj.has("offset_y")) obj.addProperty("offset_y", 1.0);
                if (!obj.has("spread")) {
                    JsonObject spread = new JsonObject();
                    spread.addProperty("x", 0.25); spread.addProperty("y", 0.5); spread.addProperty("z", 0.25);
                    obj.add("spread", spread);
                }
            }
        }
        if (ctx.action() && path.equals("side")) obj.addProperty("type", "overgrown_legacy_bridge:side");
        if (ctx == Context.ENTITY_ACTION && path.equals("modify_resource")) mergeModifiers(obj, "modifier", "modifiers");
        if (ctx == Context.POWER && path.equals("modify_movement_speed")) {
            addAttribute(obj.get("modifier"), "minecraft:generic.movement_speed");
            addAttribute(obj.get("modifiers"), "minecraft:generic.movement_speed");
            obj.addProperty("type", "apoli:attribute");
        }
        return !before.equals(obj);
    }
    private static JsonObject materialCondition(String material) {
        JsonObject out = new JsonObject(); out.addProperty("type", "apoli:in_tag"); out.addProperty("tag", "apoli:material/" + material); return out;
    }
    private static void append(JsonArray into, JsonElement values) {
        if (values.isJsonArray()) for (JsonElement value : values.getAsJsonArray()) into.add(value);
        else if (!values.isJsonNull()) into.add(values);
    }
    public static void translate(JsonObject obj, String key) {
        String value = string(obj, key);
        if (value == null) return;
        if (value.isEmpty()) { obj.remove(key); return; }
        JsonObject component = new JsonObject(); component.addProperty("translate", value); obj.add(key, component);
    }
    static void normalizeKey(JsonObject obj, String field) {
        JsonElement value = obj.get(field);
        if (value.isJsonObject()) {
            JsonObject key = value.getAsJsonObject();
            rename(key, "continous", "continuous");
            if (key.has("key")) normalizeKey(key, "key");
        } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String mapped = legacyKeyName(value.getAsString());
            if (mapped != null) obj.addProperty(field, mapped);
        }
    }
    /**
     * Overgrown Origins still registers {@code key.origins.primary_active} (G) and {@code key.origins.secondary_active};
     * {@code key.apoli.*} are separate, unbound controls. Only legacy aliases that resolve to nothing are rewritten.
     */
    static String legacyKeyName(String name) {
        return switch (name) {
            case "primary", "origins.primary_active" -> "key.origins.primary_active";
            case "secondary", "origins.secondary_active" -> "key.origins.secondary_active";
            default -> null;
        };
    }
    static boolean normalizeModifiers(JsonElement node) {
        if (node == null) return false;
        if (node.isJsonArray()) {
            boolean changed = false;
            for (JsonElement mod : node.getAsJsonArray()) changed |= normalizeModifiers(mod);
            return changed;
        }
        if (!node.isJsonObject()) return false;
        JsonObject mod = node.getAsJsonObject();
        boolean changed = false;
        String operation = string(mod, "operation");
        if (operation != null && (operation.startsWith("minecraft:") || operation.startsWith("origins:"))) {
            mod.addProperty("operation", operation.substring(operation.indexOf(':') + 1)); changed = true;
        }
        JsonElement nested = mod.get("modifier");
        if (nested != null && nested.isJsonArray() && nested.getAsJsonArray().size() == 1) {
            mod.add("modifier", nested.getAsJsonArray().get(0)); changed = true;
        }
        nested = mod.get("modifier");
        if (nested != null && nested.isJsonArray() && nested.getAsJsonArray().size() > 1) {
            // Overgrown stores one nested modifier; a legacy list travels in the marker name instead.
            boolean outer = LEGACY_MATH.get();
            LEGACY_MATH.set(true);
            try { normalizeModifiers(nested); } finally { LEGACY_MATH.set(outer); }
            markLegacy(mod, nested.getAsJsonArray());
            mod.remove("modifier");
            return true;
        }
        if (LEGACY_MATH.get()) changed |= markLegacy(mod, null);
        return normalizeModifiers(mod.get("modifier")) | changed;
    }
    /** Name prefix of modifiers computed with Apoli 2.9.0's modifier engine; the name survives client sync. */
    public static final String LEGACY_MODIFIER = "overgrown_legacy_bridge:legacy";
    private static final ThreadLocal<Boolean> LEGACY_MATH = ThreadLocal.withInitial(() -> false);
    /** Marks a modifier as legacy, keeping its own name and any nested list as a JSON payload after the prefix. */
    static boolean markLegacy(JsonObject mod, JsonArray nested) {
        String name = string(mod, "name");
        if (name != null && name.startsWith(LEGACY_MODIFIER)) return false;
        if (name == null && nested == null) { mod.addProperty("name", LEGACY_MODIFIER); return true; }
        JsonObject payload = new JsonObject();
        if (name != null) payload.addProperty("name", name);
        if (nested != null) payload.add("nested", nested.deepCopy());
        mod.addProperty("name", LEGACY_MODIFIER + payload);
        return true;
    }
    private static void addAttribute(JsonElement node, String attribute) {
        if (node == null) return;
        if (node.isJsonArray()) for (JsonElement mod : node.getAsJsonArray()) addAttribute(mod, attribute);
        else if (node.isJsonObject()) node.getAsJsonObject().addProperty("attribute", attribute);
    }
    static void mergeModifiers(JsonObject obj, String single, String plural) {
        if (obj.has("position") || obj.has("from")) return;
        // A single modifier is representable by Overgrown's action codec. Lists need a dedicated action.
        obj.addProperty("type", "overgrown_legacy_bridge:modify_resource");
    }
    static void rename(JsonObject obj, String old, String current) {
        if (!obj.has(old)) return;
        JsonElement value = obj.remove(old);
        if (!obj.has(current)) obj.add(current, value);
    }
    static String string(JsonObject obj, String field) {
        JsonElement value = obj.get(field);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() ? value.getAsString() : null;
    }
}
