package org.overgrowns.migration;

import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.FluidCtx;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.FluidState;

/** Client-side lookup shared by the vanilla chunk region and Sodium's world slice. */
public final class LegacyFluidRenderClient {
    private LegacyFluidRenderClient() {}

    /** The fluid the first matching modify_fluid_render power shows at this position, or null. */
    public static FluidState replacement(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return null;
        FluidState[] result = {null};
        PowerLookup.forEach(client.player, LegacyFluidRenderPower.ID, LegacyFluidRenderPower.Config.class, cfg -> {
            if (result[0] != null) return;
            if (cfg.blockCondition().isPresent() && !cfg.blockCondition().get().test(new BlockCtx(pos, client.level.getBlockState(pos), client.level))) return;
            if (cfg.fluidCondition().isPresent() && !cfg.fluidCondition().get().test(new FluidCtx(client.level.getFluidState(pos), pos, client.level))) return;
            if (BuiltInRegistries.FLUID.containsKey(cfg.fluid())) result[0] = BuiltInRegistries.FLUID.get(cfg.fluid()).defaultFluidState();
        });
        return result[0];
    }
}
