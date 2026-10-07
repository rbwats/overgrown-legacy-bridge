package org.overgrowns.migration;
import com.google.gson.*;
public final class LegacyOriginNormalizer {
    private LegacyOriginNormalizer() {}
    public static void origin(JsonObject root) {
        LegacySchema.translate(root, "name"); LegacySchema.translate(root, "description");
        if (root.has("powers") && root.get("powers").isJsonArray()) {
            for (JsonElement entry : root.getAsJsonArray("powers"))
                if (entry.isJsonObject()) LegacySchema.normalize(entry.getAsJsonObject().get("condition"), LegacySchema.Context.ENTITY_CONDITION);
        }
        if (root.has("upgrades") && root.get("upgrades").isJsonArray())
            for (JsonElement upgrade : root.getAsJsonArray("upgrades"))
                if (upgrade.isJsonObject()) upgrade(upgrade.getAsJsonObject());
    }
    /**
     * Origins 1.10.0 upgrades named an advancement and announced a gold translation key. Overgrown
     * expects an entity condition and polls it, so an already-earned advancement upgrades on the next check.
     */
    static void upgrade(JsonObject upgrade) {
        String advancement = LegacySchema.string(upgrade, "condition");
        if (advancement != null) {
            JsonObject condition = new JsonObject();
            condition.addProperty("type", "apoli:advancement");
            condition.addProperty("advancement", advancement);
            upgrade.add("condition", condition);
        } else if (upgrade.get("condition") != null && upgrade.get("condition").isJsonObject()) {
            LegacySchema.normalize(upgrade.get("condition"), LegacySchema.Context.ENTITY_CONDITION);
        }
        String announcement = LegacySchema.string(upgrade, "announcement");
        if (announcement != null) {
            if (announcement.isEmpty()) { upgrade.remove("announcement"); return; }
            JsonObject text = new JsonObject();
            text.addProperty("translate", announcement);
            text.addProperty("color", "gold");
            upgrade.add("announcement", text);
        }
    }
    public static void mergeLayerFields(JsonObject previous, JsonObject incoming) {
        if (incoming.has("gui_title") && incoming.get("gui_title").isJsonObject() && previous.has("gui_title") && previous.get("gui_title").isJsonObject()) {
            JsonObject merged = previous.getAsJsonObject("gui_title").deepCopy();
            incoming.getAsJsonObject("gui_title").entrySet().forEach(entry -> merged.add(entry.getKey(), entry.getValue()));
            incoming.add("gui_title", merged);
        }
        if (incoming.has("exclude_random") && incoming.get("exclude_random").isJsonArray()
                && previous.has("exclude_random") && previous.get("exclude_random").isJsonArray()
                && !bool(incoming, "replace_exclude_random")) {
            JsonArray merged = previous.getAsJsonArray("exclude_random").deepCopy();
            for (JsonElement origin : incoming.getAsJsonArray("exclude_random")) if (!merged.asList().contains(origin)) merged.add(origin);
            incoming.add("exclude_random", merged);
        }
    }
    public static void layer(JsonObject root) {
        if (!root.has("origins") || !root.get("origins").isJsonArray()) return;
        for (JsonElement entry : root.getAsJsonArray("origins"))
            if (entry.isJsonObject()) LegacySchema.normalize(entry.getAsJsonObject().get("condition"), LegacySchema.Context.ENTITY_CONDITION);
    }
    /** Legacy loading_priority of a layer, power or origin file; malformed values count as the default 0. */
    public static int loadingPriority(JsonElement json) {
        if (json == null || !json.isJsonObject()) return 0;
        JsonElement value = json.getAsJsonObject().get("loading_priority");
        try {
            return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() ? value.getAsInt() : 0;
        } catch (NumberFormatException error) {
            return 0;
        }
    }
    private static boolean bool(JsonObject obj, String key) {
        JsonElement value = obj.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean() && value.getAsBoolean();
    }
}
