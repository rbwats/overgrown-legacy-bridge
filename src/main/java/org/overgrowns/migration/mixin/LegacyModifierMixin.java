package org.overgrowns.migration.mixin;

import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.world.entity.Entity;
import org.overgrowns.migration.LegacyModifierMath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** A single legacy-marked modifier: legacy resource fallback, nested lists and add_total_late. */
@Mixin(value = AttributeModifier.class, remap = false)
public abstract class LegacyModifierMixin {
    @Inject(method = "resolveInput", at = @At("HEAD"), cancellable = true)
    private void overgrownLegacyBridge$input(Entity entity, PowerContainer container, double contextValue, CallbackInfoReturnable<Double> cir) {
        AttributeModifier self = (AttributeModifier) (Object) this;
        if (LegacyModifierMath.isLegacy(self)) cir.setReturnValue(LegacyModifierMath.input(self, entity, container, contextValue));
    }

    @Inject(method = "applyToValue", at = @At("HEAD"), cancellable = true)
    private void overgrownLegacyBridge$apply(double currentValue, Entity entity, PowerContainer container, CallbackInfoReturnable<Double> cir) {
        AttributeModifier self = (AttributeModifier) (Object) this;
        if (LegacyModifierMath.isLegacy(self)) cir.setReturnValue(LegacyModifierMath.apply(currentValue, List.of(self), entity, container));
    }
}
