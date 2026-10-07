package org.overgrowns.migration.mixin;

import com.google.gson.JsonElement;
import com.mojang.serialization.Dynamic;
import dev.overgrown.apoli.loader.ApoliReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.overgrowns.migration.LegacyEntityUse;
import org.overgrowns.migration.LegacyLoadingPriority;
import org.overgrowns.migration.LegacyPowerNormalizer;
import org.overgrowns.migration.LegacyTickRates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = ApoliReloadListener.class, remap = false)
public abstract class ApoliReloadListenerMixin {
    @Inject(method = "apply", at = @At("HEAD"))
    private void overgrownLegacyBridge$normalize(Map<ResourceLocation, JsonElement> data,
            ResourceManager manager, ProfilerFiller profiler, CallbackInfo ci) {
        LegacyLoadingPriority.select(data, manager, "powers");
        LegacyPowerNormalizer.normalizeAll(data);
        LegacyTickRates.clear();
        LegacyEntityUse.clear();
    }

    /** Every top-level power and expanded sub-power passes through here with its final id before parsing. */
    @Inject(method = "prepare(Lcom/mojang/serialization/Dynamic;Lnet/minecraft/resources/ResourceLocation;)Lcom/mojang/serialization/Dynamic;", at = @At("HEAD"))
    private static void overgrownLegacyBridge$recordLegacyFields(Dynamic<?> power, ResourceLocation id, CallbackInfoReturnable<Dynamic<?>> cir) {
        if (!(power.getValue() instanceof JsonElement json)) return;
        LegacyTickRates.record(id, json);
        LegacyEntityUse.record(id, json);
    }
}
