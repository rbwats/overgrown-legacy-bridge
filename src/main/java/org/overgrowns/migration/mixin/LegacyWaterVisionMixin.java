package org.overgrowns.migration.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.overgrowns.migration.LegacyBuiltinPowers;
import org.overgrowns.migration.LegacyIdPowers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** origins:water_vision also gave full underwater visibility, in addition to its night vision toggle. */
@Mixin(LocalPlayer.class)
public abstract class LegacyWaterVisionMixin {
    @Inject(method = "getWaterVision", at = @At("HEAD"), cancellable = true)
    private void overgrownLegacyBridge$waterVision(CallbackInfoReturnable<Float> cir) {
        if (LegacyIdPowers.active((LocalPlayer) (Object) this, LegacyBuiltinPowers.WATER_VISION)) cir.setReturnValue(1.0F);
    }
}
