package org.overgrowns.migration.mixin;

import dev.overgrown.apoli.power.builtin.ModifyHealingHandler;
import net.minecraft.world.entity.LivingEntity;
import org.overgrowns.migration.LegacyAttributeTransferPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Legacy attribute_modify_transfer with class modify_healing. */
@Mixin(value = ModifyHealingHandler.class, remap = false)
public abstract class TransferHealingMixin {
    @Inject(method = "modify", at = @At("RETURN"), cancellable = true)
    private static void overgrownLegacyBridge$transfer(LivingEntity entity, float original, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(Math.max(0f, LegacyAttributeTransferPower.apply(entity, "modify_healing", cir.getReturnValueF())));
    }
}
