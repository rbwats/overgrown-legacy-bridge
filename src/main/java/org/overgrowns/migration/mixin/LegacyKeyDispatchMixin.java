package org.overgrowns.migration.mixin;
import dev.overgrown.apoli.keybind.KeyDispatch;
import dev.overgrown.apoli.power.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.overgrowns.migration.LegacyToggleNightVisionPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = KeyDispatch.class, remap = false)
public abstract class LegacyKeyDispatchMixin {
    @Inject(method = "activateTyped", at = @At("HEAD"), cancellable = true)
    private static void overgrownLegacyBridge$activate(PowerContainer holder, ServerPlayer player, ResourceLocation id,
            Power power, String key, boolean continuousOnly, CallbackInfoReturnable<Boolean> cir) {
        if (power.config() instanceof LegacyToggleNightVisionPower.Config cfg) {
            boolean match = key == null || cfg.key().key().equals(key);
            cir.setReturnValue(!continuousOnly && match && LegacyToggleNightVisionPower.toggle(holder, id, cfg));
        }
    }
}
