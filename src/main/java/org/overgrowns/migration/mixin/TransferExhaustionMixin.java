package org.overgrowns.migration.mixin;

import dev.overgrown.apoli.power.builtin.ModifyExhaustionHandler;
import net.minecraft.world.entity.player.Player;
import org.overgrowns.migration.LegacyAttributeTransferPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Legacy attribute_modify_transfer with class modify_exhaustion. */
@Mixin(value = ModifyExhaustionHandler.class, remap = false)
public abstract class TransferExhaustionMixin {
    @Inject(method = "modify", at = @At("HEAD"))
    private static void overgrownLegacyBridge$begin(Player player, float exhaustion, CallbackInfoReturnable<Float> cir) {
        LegacyAttributeTransferPower.begin(player, "modify_exhaustion");
    }

    @Inject(method = "modify", at = @At("RETURN"), cancellable = true)
    private static void overgrownLegacyBridge$transfer(Player player, float exhaustion, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(Math.max(0f, LegacyAttributeTransferPower.end(cir.getReturnValueF())));
    }
}
