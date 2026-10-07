package org.overgrowns.migration.mixin;

import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.builtin.ModifyStatusEffectPower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.overgrowns.migration.LegacyAttributeTransferPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

/**
 * Legacy attribute_modify_transfer with classes modify_status_effect_amplifier / _duration. Each value is
 * scoped where Overgrown computes it; values Overgrown leaves alone (no power of that type) get transfers here.
 */
@Mixin(value = ModifyStatusEffectPower.class, remap = false)
public abstract class TransferStatusEffectMixin {
    @Unique private static final String AMPLIFIER = "modify_status_effect_amplifier", DURATION = "modify_status_effect_duration";
    @Unique private static final ThreadLocal<Set<String>> overgrownLegacyBridge$computed = ThreadLocal.withInitial(HashSet::new);

    @Inject(method = "apply", at = @At("HEAD"))
    private static void overgrownLegacyBridge$reset(LivingEntity entity, MobEffectInstance original, CallbackInfoReturnable<MobEffectInstance> cir) {
        overgrownLegacyBridge$computed.get().clear();
    }

    @Inject(method = "collect", at = @At("HEAD"))
    private static void overgrownLegacyBridge$begin(LivingEntity entity, ResourceLocation typeId, ResourceLocation effectId, float base, CallbackInfoReturnable<Float> cir) {
        String key = ApoliIds.MODIFY_STATUS_EFFECT_AMPLIFIER.equals(typeId) ? AMPLIFIER : DURATION;
        overgrownLegacyBridge$computed.get().add(key);
        LegacyAttributeTransferPower.begin(entity, key);
    }

    @Inject(method = "collect", at = @At("RETURN"), cancellable = true)
    private static void overgrownLegacyBridge$end(LivingEntity entity, ResourceLocation typeId, ResourceLocation effectId, float base, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(LegacyAttributeTransferPower.end(cir.getReturnValueF()));
    }

    @Inject(method = "apply", at = @At("RETURN"), cancellable = true)
    private static void overgrownLegacyBridge$transfer(LivingEntity entity, MobEffectInstance original, CallbackInfoReturnable<MobEffectInstance> cir) {
        MobEffectInstance value = cir.getReturnValue();
        if (value == null) return;
        Set<String> computed = overgrownLegacyBridge$computed.get();
        int amplifier = computed.contains(AMPLIFIER) ? value.getAmplifier()
            : Math.round(LegacyAttributeTransferPower.apply(entity, AMPLIFIER, (float) value.getAmplifier()));
        int duration = computed.contains(DURATION) ? value.getDuration()
            : Math.round(LegacyAttributeTransferPower.apply(entity, DURATION, (float) value.getDuration()));
        if (amplifier == value.getAmplifier() && duration == value.getDuration()) return;
        cir.setReturnValue(new MobEffectInstance(value.getEffect(), Math.max(0, duration), Math.max(0, amplifier),
            value.isAmbient(), value.isVisible(), value.showIcon()));
    }
}
