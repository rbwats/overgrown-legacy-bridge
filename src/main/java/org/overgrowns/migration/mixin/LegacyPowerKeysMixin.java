package org.overgrowns.migration.mixin;
import dev.overgrown.apoli.power.*;
import org.overgrowns.migration.LegacyToggleNightVisionPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Collection;
@Mixin(value = PowerKeys.class, remap = false)
public abstract class LegacyPowerKeysMixin {
    @Inject(method = "collect", at = @At("HEAD"))
    private static void overgrownLegacyBridge$collect(Power power, Collection<String> out, CallbackInfo ci) {
        if (power.config() instanceof LegacyToggleNightVisionPower.Config cfg) out.add(cfg.key().key());
    }
}
