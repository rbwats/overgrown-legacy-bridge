package org.overgrowns.migration.mixin;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.overgrowns.migration.LegacyOriginNormalizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;
@Mixin(value = dev.overgrown.origins.origin.OriginLoader.class, remap = false)
public abstract class OriginLoaderMixin {
    @Inject(method = "apply", at = @At("HEAD"))
    private void overgrownLegacyBridge$origin(Map<ResourceLocation, JsonElement> entries, ResourceManager manager, ProfilerFiller profiler, CallbackInfo ci) {
        for (JsonElement json : entries.values()) if (json.isJsonObject()) LegacyOriginNormalizer.origin(json.getAsJsonObject());
    }
}
