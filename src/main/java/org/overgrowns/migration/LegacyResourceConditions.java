package org.overgrowns.migration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/** Apoli 2.9.0's {@code apoli:any_namespace_loaded} and {@code apoli:all_namespaces_loaded} resource conditions. */
public final class LegacyResourceConditions {
    private LegacyResourceConditions() {}

    /** Namespaces of the data packs being loaded, captured when the server data resource manager is built. */
    public static volatile Set<String> loadedNamespaces = Set.of();

    public static void register() {
        register(new ResourceLocation("apoli", "any_namespace_loaded"), false);
        register(new ResourceLocation("apoli", "all_namespaces_loaded"), true);
        LegacyPowerNormalizer.loadConditions = ResourceConditions::objectMatchesConditions;
    }

    private static void register(ResourceLocation id, boolean all) {
        if (ResourceConditions.get(id) != null) return;
        ResourceConditions.register(id, json -> namespacesLoaded(json, loadedNamespaces, all));
    }

    static boolean namespacesLoaded(JsonObject json, Set<String> namespaces, boolean all) {
        JsonElement list = json.get("namespaces");
        if (list == null || !list.isJsonArray()) throw new JsonParseException("Expected a \"namespaces\" array");
        for (JsonElement entry : list.getAsJsonArray()) {
            if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString())
                throw new JsonParseException("Invalid " + entry + " entry: expected a JSON string!");
            if (namespaces.contains(entry.getAsString()) != all) return !all;
        }
        return all;
    }
}
