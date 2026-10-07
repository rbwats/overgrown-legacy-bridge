package org.overgrowns.migration.mixin;

import dev.overgrown.apoli.power.builtin.ModifySlipperinessHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import org.overgrowns.migration.LegacyAttributeTransferPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Legacy attribute_modify_transfer with class modify_slipperiness. */
@Mixin(value = ModifySlipperinessHandler.class, remap = false)
public abstract class TransferSlipperinessMixin {
    @Inject(method = "modify", at = @At("RETURN"), cancellable = true)
    private static void overgrownLegacyBridge$transfer(LivingEntity entity, BlockPos affectingPos, float original, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(LegacyAttributeTransferPower.apply(entity, "modify_slipperiness", cir.getReturnValueF()));
    }
}
