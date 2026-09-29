package org.overgrowns.migration.mixin;
import dev.overgrown.apoli.client.*;
import dev.overgrown.apoli.power.*;
import dev.overgrown.apoli.network.payload.PowerToggleC2S;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.resources.ResourceLocation;
import org.overgrowns.migration.LegacyToggleNightVisionPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value = ApoliKeyHandler.class, remap = false)
public abstract class LegacyClientKeysMixin {
    @Inject(method = "onClientTick", at = @At("RETURN"))
    private static void overgrownLegacyBridge$keys(CallbackInfo ci) {
        for (ResourceLocation id : ClientPowerState.localPowers()) {
            Power power = ApoliPowers.get(id);
            if (power == null || !(power.config() instanceof LegacyToggleNightVisionPower.Config cfg)) continue;
            var key = ApoliKeyMappings.resolve(cfg.key().key());
            if (key == null || !ApoliKeyMappings.consumePress(key, cfg.key().continuous())) continue;
            var buf = PacketByteBufs.create(); new PowerToggleC2S(id).write(buf);
            ClientPlayNetworking.send(PowerToggleC2S.CHANNEL, buf);
        }
    }
}
