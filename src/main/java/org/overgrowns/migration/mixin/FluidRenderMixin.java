package org.overgrowns.migration.mixin;
import dev.overgrown.apoli.condition.context.*;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.FluidState;
import org.overgrowns.migration.LegacyFluidRenderPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(RenderChunkRegion.class)
public abstract class FluidRenderMixin {
    @Inject(method = "getFluidState", at = @At("RETURN"), cancellable = true)
    private void overgrownLegacyBridge$fluid(BlockPos pos, CallbackInfoReturnable<FluidState> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        FluidState[] result = {null};
        PowerLookup.forEach(client.player, LegacyFluidRenderPower.ID, LegacyFluidRenderPower.Config.class, cfg -> {
            if (result[0] != null) return;
            if (cfg.blockCondition().isPresent() && !cfg.blockCondition().get().test(new BlockCtx(pos, client.level.getBlockState(pos), client.level))) return;
            if (cfg.fluidCondition().isPresent() && !cfg.fluidCondition().get().test(new FluidCtx(client.level.getFluidState(pos), pos, client.level))) return;
            if (BuiltInRegistries.FLUID.containsKey(cfg.fluid())) result[0] = BuiltInRegistries.FLUID.get(cfg.fluid()).defaultFluidState();
        });
        if (result[0] != null) cir.setReturnValue(result[0]);
    }
}
