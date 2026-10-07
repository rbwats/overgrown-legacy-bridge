package org.overgrowns.migration.mixin;

import dev.overgrown.apoli.power.builtin.ModifyStatusEffectPower;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.overgrowns.migration.LegacyAttributeTransferPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Legacy attribute_modify_transfer with classes modify_status_effect_amplifier / _duration. */
@Mixin(value = ModifyStatusEffectPower.class, remap = false)
public abstract class TransferStatusEffectMixin {
    @Inject(method = "apply", at = @At("RETURN"), cancellable = true)
    private static void overgrownLegacyBridge$transfer(LivingEntity entity, MobEffectInstance original, CallbackInfoReturnable<MobEffectInstance> cir) {
        MobEffectInstance value = cir.getReturnValue();
        if (value == null) return;
        int amplifier = Math.round(LegacyAttributeTransferPower.apply(entity, "modify_status_effect_amplifier", (float) value.getAmplifier()));
        int duration = Math.round(LegacyAttributeTransferPower.apply(entity, "modify_status_effect_duration", (float) value.getDuration()));
        if (amplifier == value.getAmplifier() && duration == value.getDuration()) return;
        cir.setReturnValue(new MobEffectInstance(value.getEffect(), Math.max(0, duration), Math.max(0, amplifier),
            value.isAmbient(), value.isVisible(), value.showIcon()));
    }
}
