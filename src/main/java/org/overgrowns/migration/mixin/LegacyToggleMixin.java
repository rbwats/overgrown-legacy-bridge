package org.overgrowns.migration.mixin;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.entity.GrabManager;
import dev.overgrown.apoli.keybind.KeyDispatch;
import dev.overgrown.apoli.network.payload.PowerToggleC2S;
import dev.overgrown.apoli.power.*;
import dev.overgrown.apoli.power.builtin.PowerStoragePower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.overgrowns.migration.LegacyToggleNightVisionPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value = Apoli.class, remap = false)
public abstract class LegacyToggleMixin {
    @Inject(method = "handleToggle", at = @At("HEAD"), cancellable = true)
    private static void overgrownLegacyBridge$toggle(ServerPlayer player, PowerToggleC2S payload, CallbackInfo ci) {
        Power power = ApoliPowers.get(payload.power());
        if (power == null || !(power.config() instanceof LegacyToggleNightVisionPower.Config cfg)) return;
        ci.cancel();
        PowerContainer holder = PowerContainer.of(player);
        if (holder == null || !holder.hasPower(payload.power()) || holder.isSuppressed(payload.power())) return;
        if (GrabManager.keybindsDisabled(player.getUUID()) || PowerStoragePower.keyMuted(holder, payload.power())) return;
        if (KeyDispatch.blocked(player, cfg.key().key())) return;
        if (power.condition().isPresent() && !power.condition().get().test(new EntityCtx(player, player.serverLevel()))) return;
        LegacyToggleNightVisionPower.toggle(holder, payload.power(), cfg);
    }
}
