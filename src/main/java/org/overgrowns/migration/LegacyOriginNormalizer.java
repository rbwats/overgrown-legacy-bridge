package org.overgrowns.migration;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
public final class LegacyOriginNormalizer {
    private LegacyOriginNormalizer() {}
    public static void origin(JsonObject root) {
        LegacySchema.translate(root, "name"); LegacySchema.translate(root, "description");
        if (root.has("powers") && root.get("powers").isJsonArray()) {
            for (JsonElement entry : root.getAsJsonArray("powers"))
                if (entry.isJsonObject()) LegacySchema.normalize(entry.getAsJsonObject().get("condition"), LegacySchema.Context.ENTITY_CONDITION);
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
                && !(incoming.has("replace_exclude_random") && incoming.get("replace_exclude_random").getAsBoolean())) {
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
}
