package org.overgrowns.migration.mixin;
import dev.overgrown.apoli.condition.builtin.entity.PowerActiveCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.*;
import org.overgrowns.migration.LegacyToggleNightVisionPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = PowerActiveCondition.class, remap = false)
public abstract class LegacyPowerActiveMixin {
    @Inject(method = "test(Ldev/overgrown/apoli/condition/builtin/entity/PowerActiveCondition$Cfg;Ldev/overgrown/apoli/condition/context/EntityCtx;)Z", at = @At("RETURN"), cancellable = true)
    private void overgrownLegacyBridge$active(PowerActiveCondition.Cfg cfg, EntityCtx ctx, CallbackInfoReturnable<Boolean> cir) {
        Power power = ApoliPowers.get(cfg.power());
        if (cir.getReturnValueZ() && power != null && power.config() instanceof LegacyToggleNightVisionPower.Config toggle)
            cir.setReturnValue(LegacyToggleNightVisionPower.enabled(PowerContainer.of(ctx.entity()), cfg.power(), toggle));
    }
}
