package org.overgrowns.migration.mixin;

import dev.overgrown.apoli.power.builtin.ModifyBreakSpeedHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.overgrowns.migration.LegacyAttributeTransferPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Legacy attribute_modify_transfer with class modify_break_speed, on the final break progress as legacy did. */
@Mixin(value = ModifyBreakSpeedHandler.class, remap = false)
public abstract class TransferBreakSpeedMixin {
    @Inject(method = "modifySpeed", at = @At("RETURN"), cancellable = true)
    private static void overgrownLegacyBridge$transfer(float speed, Player player, BlockGetter level, BlockPos pos, BlockState state, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(LegacyAttributeTransferPower.apply(player, "modify_break_speed", cir.getReturnValueF()));
    }
}
