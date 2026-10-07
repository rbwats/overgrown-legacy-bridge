package org.overgrowns.migration.mixin;

import dev.overgrown.apoli.power.builtin.ModifyJumpHandler;
import net.minecraft.world.entity.LivingEntity;
import org.overgrowns.migration.LegacyAttributeTransferPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Legacy attribute_modify_transfer with class modify_jump; RETURN also covers the "no powers" exit. */
@Mixin(value = ModifyJumpHandler.class, remap = false)
public abstract class TransferJumpMixin {
    @Inject(method = "modify", at = @At("HEAD"))
    private static void overgrownLegacyBridge$begin(LivingEntity entity, float original, CallbackInfoReturnable<Float> cir) {
        LegacyAttributeTransferPower.begin(entity, "modify_jump");
    }

    @Inject(method = "modify", at = @At("RETURN"), cancellable = true)
    private static void overgrownLegacyBridge$transfer(LivingEntity entity, float original, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(LegacyAttributeTransferPower.end(cir.getReturnValueF()));
    }
}
