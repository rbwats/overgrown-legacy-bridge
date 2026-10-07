package org.overgrowns.migration.mixin;

import dev.overgrown.apoli.power.builtin.ModifyVelocityHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.overgrowns.migration.LegacyAttributeTransferPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Legacy attribute_modify_transfer with class modify_velocity: each axis of self-movement; one pass per axis. */
@Mixin(value = ModifyVelocityHandler.class, remap = false)
public abstract class TransferVelocityMixin {
    @Inject(method = "modify", at = @At("HEAD"))
    private static void overgrownLegacyBridge$begin(Entity entity, Vec3 original, CallbackInfoReturnable<Vec3> cir) {
        LegacyAttributeTransferPower.begin(entity, "modify_velocity");
    }

    @Inject(method = "modify", at = @At("RETURN"), cancellable = true)
    private static void overgrownLegacyBridge$transfer(Entity entity, Vec3 original, CallbackInfoReturnable<Vec3> cir) {
        if (LegacyAttributeTransferPower.close()) return;
        Vec3 value = cir.getReturnValue();
        Vec3 transferred = new Vec3(
            LegacyAttributeTransferPower.apply(entity, "modify_velocity", value.x),
            LegacyAttributeTransferPower.apply(entity, "modify_velocity", value.y),
            LegacyAttributeTransferPower.apply(entity, "modify_velocity", value.z));
        if (!transferred.equals(value)) cir.setReturnValue(transferred);
    }
}
