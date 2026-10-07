package org.overgrowns.migration.mixin;

import dev.overgrown.apoli.power.builtin.ModifyProjectileDamageHandler;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.overgrowns.migration.LegacyAttributeTransferPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Legacy attribute_modify_transfer with class modify_projectile_damage: the attacker's transfers, projectile damage only. */
@Mixin(value = ModifyProjectileDamageHandler.class, remap = false)
public abstract class TransferProjectileDamageMixin {
    @Inject(method = {"modifyAmount", "previewAmount"}, at = @At("RETURN"), cancellable = true)
    private static void overgrownLegacyBridge$transfer(LivingEntity shooter, LivingEntity target, DamageSource source, float amount, Level level, CallbackInfoReturnable<Float> cir) {
        if (source.getEntity() == null || !source.is(DamageTypeTags.IS_PROJECTILE)) return;
        cir.setReturnValue(Math.max(0f, LegacyAttributeTransferPower.apply(source.getEntity(), "modify_projectile_damage", cir.getReturnValueF())));
    }
}
