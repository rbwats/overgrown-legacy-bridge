package org.overgrowns.migration.mixin;
import dev.overgrown.apoli.power.builtin.NightVisionPower;
import net.minecraft.world.entity.LivingEntity;
import org.overgrowns.migration.LegacyToggleNightVisionPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = NightVisionPower.class, remap = false)
public abstract class LegacyNightVisionMixin {
    @Inject(method = "strengthFor", at = @At("RETURN"), cancellable = true)
    private static void overgrownLegacyBridge$strength(LivingEntity entity, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(Math.max(cir.getReturnValueF(), LegacyToggleNightVisionPower.strengthFor(entity)));
    }
}
