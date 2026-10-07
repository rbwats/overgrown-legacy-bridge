package org.overgrowns.migration;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Legacy Origins and Apoli loaded every pack's copy of a file and kept the highest {@code loading_priority}.
 * The fork's single-file loaders keep the top pack only. Here a lower pack wins only when its priority is
 * strictly higher than the copy already chosen, so ordinary overrides keep the usual top-pack behavior.
 */
public final class LegacyLoadingPriority {
    private LegacyLoadingPriority() {}

    public static int select(Map<ResourceLocation, JsonElement> data, ResourceManager manager, String directory) {
        int replaced = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            try {
                ResourceLocation id = entry.getKey();
                List<Resource> stack = manager.getResourceStack(
                    new ResourceLocation(id.getNamespace(), directory + "/" + id.getPath() + ".json"));
                if (stack.size() < 2) continue;
                JsonElement chosen = entry.getValue();
                int best = LegacyOriginNormalizer.loadingPriority(chosen);
                for (Resource resource : stack) {
                    JsonElement candidate = read(resource);
                    if (candidate == null || !candidate.isJsonObject() || candidate.equals(chosen)) continue;
                    int priority = LegacyOriginNormalizer.loadingPriority(candidate);
                    if (priority <= best || !LegacyPowerNormalizer.loadConditions.test(candidate.getAsJsonObject())) continue;
                    chosen = candidate;
                    best = priority;
                }
                if (chosen != entry.getValue()) {
                    entry.setValue(chosen);
                    replaced++;
                }
            } catch (RuntimeException error) {
                LegacyBridge.LOGGER.warn("Could not compare loading priorities for {} {}: {}", directory, entry.getKey(), error.toString());
            }
        }
        if (replaced > 0) LegacyBridge.LOGGER.info("Legacy loading_priority selected a lower pack's copy for {} {} file(s)", replaced, directory);
        return replaced;
    }

    /** Orders an origin layer's resource stack by loading_priority, keeping pack order among equal priorities. */
    public static List<Resource> sortLayerStack(List<Resource> stack) {
        if (stack == null || stack.size() < 2) return stack;
        List<Resource> sorted = new ArrayList<>(stack);
        Map<Resource, Integer> priorities = new java.util.IdentityHashMap<>();
        boolean any = false;
        for (Resource resource : sorted) {
            int priority = LegacyOriginNormalizer.loadingPriority(read(resource));
            priorities.put(resource, priority);
            any |= priority != 0;
        }
        if (!any) return stack;
        sorted.sort(Comparator.comparingInt(priorities::get));
        return sorted;
    }

    private static JsonElement read(Resource resource) {
        try (Reader reader = resource.openAsReader()) {
            return JsonParser.parseReader(reader);
        } catch (Exception error) {
            return null;
        }
    }
}
