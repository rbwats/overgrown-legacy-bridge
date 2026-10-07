package org.overgrowns.migration.mixin;

import dev.overgrown.apoli.power.builtin.ModifyAirSpeedPower;
import net.minecraft.world.entity.LivingEntity;
import org.overgrowns.migration.LegacyAttributeTransferPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Legacy transfer modifiers joined the air speed modifiers; covers every return path, including "no powers". */
@Mixin(value = ModifyAirSpeedPower.class, remap = false)
public abstract class ModifyAirSpeedMixin {
    @Inject(method = "modify", at = @At("RETURN"), cancellable = true)
    private static void overgrownLegacyBridge$transfer(LivingEntity entity, float original, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(LegacyAttributeTransferPower.modifyAirSpeed(entity, cir.getReturnValueF()));
    }
}
