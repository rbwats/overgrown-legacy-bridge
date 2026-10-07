package org.overgrowns.migration.mixin;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.overgrowns.migration.LegacyLoadingPriority;
import org.overgrowns.migration.LegacyOriginNormalizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;
@Mixin(value = dev.overgrown.origins.origin.OriginLayerLoader.class, remap = false)
public abstract class OriginLayerLoaderMixin {
    @Unique private static volatile java.util.Map<ResourceLocation, Integer> overgrownLegacyBridge$indices = java.util.Map.of();
    @Inject(method = {
        "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        "apply(Ljava/util/Map;Lnet/minecraft/class_3300;Lnet/minecraft/class_3695;)V"}, at = @At("HEAD"))
    private void overgrownLegacyBridge$indices(java.util.Map<ResourceLocation, com.google.gson.JsonElement> entries,
            net.minecraft.server.packs.resources.ResourceManager manager, net.minecraft.util.profiling.ProfilerFiller profiler,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        overgrownLegacyBridge$indices = LegacyOriginNormalizer.legacyLayerIndices(entries.keySet());
    }
    @Inject(method = "mergeInto", at = @At("HEAD"))
    private static void overgrownLegacyBridge$merge(JsonObject previous, JsonObject incoming, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        LegacyOriginNormalizer.mergeLayerFields(previous, incoming);
    }
    /** Legacy layers merged in ascending loading_priority order; replace discarded lower priorities. */
    @ModifyVariable(method = "mergeStack", at = @At("HEAD"), argsOnly = true)
    private static List<Resource> overgrownLegacyBridge$priority(List<Resource> stack) {
        return LegacyLoadingPriority.sortLayerStack(stack);
    }
    @Inject(method = "mergeStack", at = @At("RETURN"))
    private static void overgrownLegacyBridge$layer(List<Resource> stack, ResourceLocation id, CallbackInfoReturnable<JsonObject> cir) {
        if (cir.getReturnValue() == null) return;
        try {
            LegacyOriginNormalizer.layer(cir.getReturnValue());
            LegacyOriginNormalizer.defaultLayerOrder(cir.getReturnValue(), overgrownLegacyBridge$indices.get(id));
        } catch (RuntimeException error) {
            org.slf4j.LoggerFactory.getLogger("overgrown_legacy_bridge").error("Legacy normalization failed for origin layer {}: {}", id, error.toString());
        }
    }
}
