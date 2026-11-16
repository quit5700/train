package cn.quit5700.fabrictrain.block;

import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import cn.quit5700.fabrictrain.power.RailPowerService;

public class SignalRailBlock extends RailBlock {
    public SignalRailBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (level instanceof ServerLevel serverLevel) RailPowerService.markDirty(serverLevel);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos,
                                      BlockState replacement, boolean moved) {
        super.onRemove(state, level, pos, replacement, moved);
        if (level instanceof ServerLevel serverLevel) RailPowerService.markDirty(serverLevel);
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block source,
                                             BlockPos sourcePos, boolean moved) {
        super.neighborChanged(state, level, pos, source, sourcePos, moved);
        if (level instanceof ServerLevel serverLevel) RailPowerService.markDirty(serverLevel);
    }
}
