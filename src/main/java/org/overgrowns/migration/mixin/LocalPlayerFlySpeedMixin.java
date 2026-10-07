package org.overgrowns.migration.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.power.builtin.ModifyAirSpeedPower;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Legacy modify_air_speed (and its transfers) also scaled creative flight's vertical speed. */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerFlySpeedMixin {
    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Abilities;getFlyingSpeed()F"), require = 0)
    private float overgrownLegacyBridge$flySpeed(float original) {
        return ModifyAirSpeedPower.modify((LocalPlayer) (Object) this, original);
    }
}
