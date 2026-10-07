package org.overgrowns.migration.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.power.builtin.ModifyVelocityHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.overgrowns.migration.LegacyAttributeTransferPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Legacy attribute_modify_transfer with class modify_velocity: legacy ran one modify call per axis, so every axis
 * takes the transfers, including axes without velocity modifiers of their own.
 */
@Mixin(value = ModifyVelocityHandler.class, remap = false)
public abstract class TransferVelocityMixin {
    @Inject(method = "modify", at = @At("HEAD"))
    private static void overgrownLegacyBridge$begin(Entity entity, Vec3 original, CallbackInfoReturnable<Vec3> cir) {
        LegacyAttributeTransferPower.beginEveryPass(entity, "modify_velocity");
    }

    // Overgrown's three-argument apply returns early for an empty list, so that axis never reaches a merged pass.
    @WrapOperation(method = "modify", at = {
        @At(value = "INVOKE", target = "Ldev/overgrown/apoli/data/AttributeModifierHelper;apply(DLjava/util/List;Lnet/minecraft/world/entity/Entity;)D"),
        @At(value = "INVOKE", target = "Ldev/overgrown/apoli/data/AttributeModifierHelper;apply(DLjava/util/List;Lnet/minecraft/class_1297;)D")})
    private static double overgrownLegacyBridge$axis(double value, List<AttributeModifier> mods, Entity living, Operation<Double> original) {
        return mods.isEmpty() ? LegacyAttributeTransferPower.applyScoped(value) : original.call(value, mods, living);
    }

    @Inject(method = "modify", at = @At("RETURN"), cancellable = true)
    private static void overgrownLegacyBridge$transfer(Entity entity, Vec3 original, CallbackInfoReturnable<Vec3> cir) {
        // Merged: the handler reached its per-axis passes. Otherwise it returned early without velocity modifiers.
        if (LegacyAttributeTransferPower.close()) return;
        Vec3 value = cir.getReturnValue();
        Vec3 transferred = new Vec3(
            LegacyAttributeTransferPower.apply(entity, "modify_velocity", value.x),
            LegacyAttributeTransferPower.apply(entity, "modify_velocity", value.y),
            LegacyAttributeTransferPower.apply(entity, "modify_velocity", value.z));
        if (!transferred.equals(value)) cir.setReturnValue(transferred);
    }
}
