package org.overgrowns.migration;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** A cobweb placed by legacy Origins functions which decays after a few seconds. */
public final class TemporaryCobwebBlock extends Block {
    public TemporaryCobwebBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.COBWEB));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this)) {
            level.scheduleTick(pos, this, 100);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockState(pos).is(this)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }
}
