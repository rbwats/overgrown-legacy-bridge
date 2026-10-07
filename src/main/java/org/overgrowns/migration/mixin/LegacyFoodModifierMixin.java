package org.overgrowns.migration.mixin;

import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.ModifyFoodHandler;
import net.minecraft.world.entity.LivingEntity;
import org.overgrowns.migration.LegacyModifierMath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

/** Overgrown applies a power's food modifiers one by one; legacy lists are computed as one legacy pass. */
@Mixin(value = ModifyFoodHandler.class, remap = false)
public abstract class LegacyFoodModifierMixin {
    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private static void overgrownLegacyBridge$legacy(Optional<List<AttributeModifier>> modifiers, double value, LivingEntity entity,
                                                     PowerContainer container, CallbackInfoReturnable<Double> cir) {
        if (modifiers.isPresent() && LegacyModifierMath.allLegacy(modifiers.get()))
            cir.setReturnValue(LegacyModifierMath.apply(value, modifiers.get(), entity, container));
    }
}
