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
        double value = cir.getReturnValueD();
        // Legacy applied modify_falling transfers only while a falling power was active and the entity was not rising.
        if (entity.getDeltaMovement().y <= 0 && dev.overgrown.apoli.power.PowerLookup.hasActive(entity, dev.overgrown.apoli.power.ApoliIds.MODIFY_FALLING))
            value = org.overgrowns.migration.LegacyAttributeTransferPower.apply(entity, "modify_falling", value);
        cir.setReturnValue(LegacyGravityPower.modify(entity, value));
    }
}
