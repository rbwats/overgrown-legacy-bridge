package org.overgrowns.migration.mixin;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.overgrowns.migration.LegacyLoadingPriority;
import org.overgrowns.migration.LegacyOriginNormalizer;
import org.overgrowns.migration.LegacyOriginUpgrades;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;
@Mixin(value = dev.overgrown.origins.origin.OriginLoader.class, remap = false)
public abstract class OriginLoaderMixin {
    @Inject(method = {
        "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        "method_18788(Ljava/util/Map;Lnet/minecraft/class_3300;Lnet/minecraft/class_3695;)V"}, at = @At("HEAD"))
    private void overgrownLegacyBridge$origin(Map<ResourceLocation, JsonElement> entries, ResourceManager manager, ProfilerFiller profiler, CallbackInfo ci) {
        LegacyLoadingPriority.select(entries, manager, "origins");
        Map<ResourceLocation, java.util.List<JsonObject>> upgrades = new java.util.HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            try {
                var legacy = LegacyOriginNormalizer.extractAdvancementUpgrades(entry.getValue().getAsJsonObject());
                if (!legacy.isEmpty()) upgrades.put(entry.getKey(), legacy);
                LegacyOriginNormalizer.origin(entry.getValue().getAsJsonObject());
            } catch (RuntimeException error) {
                org.slf4j.LoggerFactory.getLogger("overgrown_legacy_bridge").error("Legacy normalization failed for origin {}: {}", entry.getKey(), error.toString());
            }
        }
        LegacyOriginUpgrades.load(upgrades);
    }
}
