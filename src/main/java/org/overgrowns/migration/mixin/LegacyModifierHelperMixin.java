package org.overgrowns.migration.mixin;

import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.world.entity.Entity;
import org.overgrowns.migration.LegacyAttributeTransferPower;
import org.overgrowns.migration.LegacyModifierMath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists made only of legacy-marked modifiers are computed with Apoli 2.9.0's engine, together with any
 * attribute_modify_transfer modifiers of the handler being run. Everything else keeps Overgrown's math.
 * remap = false: Overgrown's class; the descriptor naming Entity is listed in both mappings.
 */
@Mixin(value = AttributeModifierHelper.class, remap = false)
public abstract class LegacyModifierHelperMixin {
    @Inject(method = {
        "apply(DLjava/util/List;Lnet/minecraft/world/entity/Entity;Ldev/overgrown/apoli/power/PowerContainer;)D",
        "apply(DLjava/util/List;Lnet/minecraft/class_1297;Ldev/overgrown/apoli/power/PowerContainer;)D"},
        at = @At("HEAD"), cancellable = true)
    private static void overgrownLegacyBridge$legacy(double baseValue, List<AttributeModifier> mods, Entity entity, PowerContainer container,
                                                     CallbackInfoReturnable<Double> cir) {
        if (!LegacyModifierMath.allLegacy(mods)) return;
        List<org.overgrowns.migration.LegacyModifierEngine.Term> terms = new ArrayList<>(LegacyModifierMath.terms(mods, entity, container, baseValue));
        terms.addAll(LegacyAttributeTransferPower.mergeTerms(entity));
        double result = LegacyModifierMath.apply(baseValue, terms);
        cir.setReturnValue(Double.isFinite(result) ? result : baseValue);
    }

    @Inject(method = {
        "apply(DLjava/util/List;Lnet/minecraft/world/entity/Entity;Ldev/overgrown/apoli/power/PowerContainer;)D",
        "apply(DLjava/util/List;Lnet/minecraft/class_1297;Ldev/overgrown/apoli/power/PowerContainer;)D"},
        at = @At("RETURN"), cancellable = true)
    private static void overgrownLegacyBridge$transfersAfter(double baseValue, List<AttributeModifier> mods, Entity entity, PowerContainer container,
                                                             CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(LegacyAttributeTransferPower.afterPass(entity, cir.getReturnValueD()));
    }

    @Inject(method = "applyOwned", at = @At("HEAD"), cancellable = true)
    private static void overgrownLegacyBridge$legacyOwned(double baseValue, List<AttributeModifierHelper.Owned> mods, CallbackInfoReturnable<Double> cir) {
        if (!LegacyModifierMath.allLegacyOwned(mods)) return;
        double result = LegacyModifierMath.applyOwned(baseValue, mods);
        cir.setReturnValue(Double.isFinite(result) ? result : baseValue);
    }
}
