package org.overgrowns.migration.mixin;

import dev.overgrown.apoli.power.builtin.ModifyDamageHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.overgrowns.migration.LegacyDamageContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ModifyDamageHandler.class, remap = false)
public abstract class ModifyDamageHandlerMixin {
    @Inject(method = "modifyAmount", at = @At("HEAD"))
    private static void overgrownLegacyBridge$pushDamage(LivingEntity attacker, LivingEntity target,
            DamageSource source, float amount, Level level, CallbackInfoReturnable<Float> cir) {
        LegacyDamageContext.push(target, amount);
    }

    @Inject(method = "modifyAmount", at = @At("RETURN"))
    private static void overgrownLegacyBridge$popDamage(LivingEntity attacker, LivingEntity target,
            DamageSource source, float amount, Level level, CallbackInfoReturnable<Float> cir) {
        LegacyDamageContext.pop();
    }
}
