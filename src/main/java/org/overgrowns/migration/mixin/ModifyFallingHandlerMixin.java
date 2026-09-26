package org.overgrowns.migration.mixin;

import dev.overgrown.apoli.power.builtin.ModifyFallingHandler;
import net.minecraft.world.entity.LivingEntity;
import org.overgrowns.migration.LegacyGravityPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Runs after Apoli's falling modifier so both power systems compose. */
@Mixin(value = ModifyFallingHandler.class, remap = false)
public abstract class ModifyFallingHandlerMixin {
    @Inject(method = "modifyGravity", at = @At("RETURN"), cancellable = true)
    private static void overgrownLegacyBridge$gravity(LivingEntity entity, double vanilla,
            CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(LegacyGravityPower.modify(entity, cir.getReturnValue()));
    }
}
