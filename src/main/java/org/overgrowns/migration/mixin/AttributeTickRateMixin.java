package org.overgrowns.migration.mixin;

import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.AttributePower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.overgrowns.migration.LegacyTickRates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Legacy conditioned_attribute re-checked its condition every tick_rate ticks; origins:attribute never did. */
@Mixin(value = AttributePower.class, remap = false)
public abstract class AttributeTickRateMixin {
    // Development and production (intermediary) spellings of the same descriptor.
    @Inject(method = {
        "tick(Lnet/minecraft/resources/ResourceLocation;Ldev/overgrown/apoli/power/builtin/AttributePower$Cfg;Ldev/overgrown/apoli/power/PowerContainer;)V",
        "tick(Lnet/minecraft/class_2960;Ldev/overgrown/apoli/power/builtin/AttributePower$Cfg;Ldev/overgrown/apoli/power/PowerContainer;)V"},
        at = @At("HEAD"), cancellable = true)
    private void overgrownLegacyBridge$tickRate(ResourceLocation powerId, AttributePower.Cfg cfg, PowerContainer holder, CallbackInfo ci) {
        if (!LegacyTickRates.attributeTicks(powerId, holder.owner())) ci.cancel();
    }

    @Inject(method = "conditionHolds", at = @At("HEAD"), cancellable = true)
    private static void overgrownLegacyBridge$unconditional(LivingEntity entity, ResourceLocation powerId, CallbackInfoReturnable<Boolean> cir) {
        if (LegacyTickRates.ignoresCondition(powerId)) cir.setReturnValue(true);
    }
}
