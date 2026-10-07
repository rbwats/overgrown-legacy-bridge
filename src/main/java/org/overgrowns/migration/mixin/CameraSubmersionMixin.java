package org.overgrowns.migration.mixin;
import dev.overgrown.apoli.power.builtin.ModifyCameraSubmersionPower;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.overgrowns.migration.LegacyCameraSubmersionPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = ModifyCameraSubmersionPower.class, remap = false)
public abstract class CameraSubmersionMixin {
    @Inject(method = "remap", at = @At("RETURN"), cancellable = true)
    private static void overgrownLegacyBridge$remap(Entity entity, FogType original, CallbackInfoReturnable<FogType> cir) {
        cir.setReturnValue(LegacyCameraSubmersionPower.remap(entity, original, cir.getReturnValue()));
    }
}
