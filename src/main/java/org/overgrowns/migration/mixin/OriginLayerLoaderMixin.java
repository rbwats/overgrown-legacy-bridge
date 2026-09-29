package org.overgrowns.migration.mixin;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.overgrowns.migration.LegacyOriginNormalizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;
@Mixin(value = dev.overgrown.origins.origin.OriginLayerLoader.class, remap = false)
public abstract class OriginLayerLoaderMixin {
    @Inject(method = "mergeInto", at = @At("HEAD"))
    private static void overgrownLegacyBridge$merge(JsonObject previous, JsonObject incoming, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        LegacyOriginNormalizer.mergeLayerFields(previous, incoming);
    }
    @Inject(method = "mergeStack", at = @At("RETURN"))
    private static void overgrownLegacyBridge$layer(List<Resource> stack, ResourceLocation id, CallbackInfoReturnable<JsonObject> cir) {
        if (cir.getReturnValue() != null) LegacyOriginNormalizer.layer(cir.getReturnValue());
    }
}
