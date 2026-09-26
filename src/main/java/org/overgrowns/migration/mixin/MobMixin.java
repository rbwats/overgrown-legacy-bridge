package org.overgrowns.migration.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.overgrowns.migration.LegacyMobsIgnorePower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void overgrownLegacyBridge$preventTarget(LivingEntity target, CallbackInfo ci) {
        if (target != null && LegacyMobsIgnorePower.blocks((Mob) (Object) this, target)) ci.cancel();
    }

    @Inject(method = "getTarget", at = @At("RETURN"), cancellable = true)
    private void overgrownLegacyBridge$dropTarget(CallbackInfoReturnable<LivingEntity> cir) {
        LivingEntity target = cir.getReturnValue();
        Mob mob = (Mob) (Object) this;
        if (target != null && LegacyMobsIgnorePower.blocks(mob, target)) {
            if (!mob.level().isClientSide()) mob.setTarget(null);
            cir.setReturnValue(null);
        }
    }
}
