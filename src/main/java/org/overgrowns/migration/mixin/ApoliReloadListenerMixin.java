package org.overgrowns.migration.mixin;

import com.google.gson.JsonElement;
import dev.overgrown.apoli.loader.ApoliReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.overgrowns.migration.LegacyPowerNormalizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = ApoliReloadListener.class, remap = false)
public abstract class ApoliReloadListenerMixin {
    @Inject(method = "apply", at = @At("HEAD"))
    private void overgrownLegacyBridge$normalize(Map<ResourceLocation, JsonElement> data,
            ResourceManager manager, ProfilerFiller profiler, CallbackInfo ci) {
        LegacyPowerNormalizer.normalizeAll(data);
    }
}
