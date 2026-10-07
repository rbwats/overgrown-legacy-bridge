package org.overgrowns.migration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;

/**
 * Origins 1.10.0 attached Java behavior to some built-in power IDs whose JSON is only {@code origins:simple}.
 * Overgrown moved its built-ins into subfolders, so the bridge ships the legacy files under their original IDs
 * (see {@code data/origins/powers}) and gives the ID-keyed placeholders equivalent native types here.
 * Cobweb slowdown, conduit power on land and underwater visibility stay keyed by ID in bridge mixins.
 */
public final class LegacyBuiltinPowers {
    private LegacyBuiltinPowers() {}

    public static final ResourceLocation NO_COBWEB_SLOWDOWN = new ResourceLocation("origins", "no_cobweb_slowdown");
    public static final ResourceLocation MASTER_OF_WEBS_NO_SLOWDOWN = new ResourceLocation("origins", "master_of_webs_no_slowdown");
    public static final ResourceLocation CONDUIT_POWER_ON_LAND = new ResourceLocation("origins", "conduit_power_on_land");
    public static final ResourceLocation WATER_VISION = new ResourceLocation("origins", "water_vision");

    private static final Set<String> RETAINED = Set.of("name", "description", "hidden", "loading_priority", "badges", "condition");

    private static final Map<String, String> REPLACEMENTS = Map.of(
        "water_breathing", """
            {"type":"apoli:water_breathing","suffocate_outside_water":true}""",
        // FleeEntityGoal(6 blocks, 1.0 walk, 1.2 sprint) plus a creeper target goal that skipped the holder.
        "scare_creepers", """
            {"type":"apoli:multiple",
             "flee":{"type":"apoli:scare_mobs","radius":6.0,"speed":1.2,
               "bientity_condition":{"type":"apoli:target_condition","condition":{"type":"apoli:entity_type","entity_type":"minecraft:creeper"}}},
             "ignore":{"type":"overgrown_legacy_bridge:mobs_ignore","provokable":true,
               "mob_condition":{"type":"apoli:entity_type","entity_type":"minecraft:creeper"}}}""",
        // Removed the downward drift while swimming unless the player actively descends.
        "like_water", """
            {"type":"apoli:modify_falling","velocity":0.0,"take_fall_damage":true,
             "condition":{"type":"apoli:and","conditions":[
               {"type":"apoli:fluid_height","fluid":"minecraft:water","comparison":">","compare_to":0.0},
               {"type":"apoli:sneaking","inverted":true}]}}"""
    );

    /** Replaces an ID-keyed legacy placeholder with its native equivalent. Returns whether the JSON changed. */
    public static boolean adapt(ResourceLocation id, JsonObject json) {
        if (!"origins".equals(id.getNamespace())) return false;
        String type = LegacySchema.string(json, "type");
        if (!"origins:simple".equals(type) && !"apoli:simple".equals(type)) return false;
        String template = REPLACEMENTS.get(id.getPath());
        if (template == null) return false;
        JsonObject replacement = JsonParser.parseString(template).getAsJsonObject();
        JsonElement condition = json.get("condition");
        boolean multiple = LegacySchema.string(replacement, "type").endsWith(":multiple");
        for (String key : RETAINED) {
            if (!json.has(key)) continue;
            if (key.equals("condition")) continue;
            replacement.add(key, json.get(key).deepCopy());
        }
        if (condition != null) {
            if (multiple) {
                for (Map.Entry<String, JsonElement> sub : replacement.entrySet())
                    if (sub.getValue().isJsonObject() && sub.getValue().getAsJsonObject().has("type"))
                        addCondition(sub.getValue().getAsJsonObject(), condition);
            } else {
                addCondition(replacement, condition);
            }
        }
        for (String key : Set.copyOf(json.keySet())) json.remove(key);
        for (Map.Entry<String, JsonElement> entry : replacement.entrySet()) json.add(entry.getKey(), entry.getValue());
        return true;
    }

    private static void addCondition(JsonObject power, JsonElement condition) {
        JsonElement existing = power.get("condition");
        if (existing == null) { power.add("condition", condition.deepCopy()); return; }
        JsonArray both = new JsonArray();
        both.add(existing);
        both.add(condition.deepCopy());
        JsonObject and = new JsonObject();
        and.addProperty("type", "apoli:and");
        and.add("conditions", both);
        power.add("condition", and);
    }
}
