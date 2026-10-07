package org.overgrowns.migration.mixin;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.overgrowns.migration.LegacyFluidRenderClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(RenderChunkRegion.class)
public abstract class FluidRenderMixin {
    @Inject(method = "getFluidState", at = @At("RETURN"), cancellable = true)
    private void overgrownLegacyBridge$fluid(BlockPos pos, CallbackInfoReturnable<FluidState> cir) {
        FluidState replacement = LegacyFluidRenderClient.replacement(pos);
        if (replacement != null) cir.setReturnValue(replacement);
    }
}
