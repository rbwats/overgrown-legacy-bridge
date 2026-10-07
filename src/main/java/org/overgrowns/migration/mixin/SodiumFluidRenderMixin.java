package org.overgrowns.migration.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.overgrowns.migration.LegacyFluidRenderClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sodium 0.5 builds chunk meshes from its WorldSlice instead of vanilla's RenderChunkRegion.
 * The bridge is not compiled against Sodium, so the target is named as it is at runtime:
 * method_8316 is BlockGetter.getFluidState(BlockPos) in intermediary.
 */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.world.WorldSlice", remap = false)
public abstract class SodiumFluidRenderMixin {
    @Inject(method = {"method_8316", "getFluidState"}, at = @At("RETURN"), cancellable = true, require = 0)
    private void overgrownLegacyBridge$fluid(BlockPos pos, CallbackInfoReturnable<FluidState> cir) {
        FluidState replacement = LegacyFluidRenderClient.replacement(pos);
        if (replacement != null) cir.setReturnValue(replacement);
    }
}
