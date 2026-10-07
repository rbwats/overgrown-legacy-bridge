package org.overgrowns.migration.mixin;

import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.AttributePower;
import net.minecraft.resources.ResourceLocation;
import org.overgrowns.migration.LegacyTickRates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Legacy conditioned_attribute only re-checked its condition every tick_rate ticks. */
@Mixin(value = AttributePower.class, remap = false)
public abstract class AttributeTickRateMixin {
    @Inject(method = "tick(Lnet/minecraft/resources/ResourceLocation;Ldev/overgrown/apoli/power/builtin/AttributePower$Cfg;Ldev/overgrown/apoli/power/PowerContainer;)V", at = @At("HEAD"), cancellable = true)
    private void overgrownLegacyBridge$tickRate(ResourceLocation powerId, AttributePower.Cfg cfg, PowerContainer holder, CallbackInfo ci) {
        if (!LegacyTickRates.attributeTicks(powerId, holder.owner())) ci.cancel();
    }
}
