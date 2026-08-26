package cn.quit5700.fabrictrain.block;

import cn.quit5700.fabrictrain.block.entity.PowerGeneratorBlockEntity;
import cn.quit5700.fabrictrain.power.RailPowerService;
import cn.quit5700.fabrictrain.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.Block;

public final class PowerGeneratorBlock extends BaseEntityBlock {
    public static final MapCodec<PowerGeneratorBlock> CODEC = simpleCodec(PowerGeneratorBlock::new);
    public PowerGeneratorBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PowerGeneratorBlockEntity(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected boolean isSignalSource(BlockState state) { return true; }
    @Override protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof PowerGeneratorBlockEntity generator && generator.active() ? 15 : 0;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PowerGeneratorBlockEntity generator) player.openMenu(generator);
        return InteractionResult.SUCCESS;
    }

    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel serverLevel) RailPowerService.removeGenerator(serverLevel, pos);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PowerGeneratorBlockEntity generator) Containers.dropContents(level, pos, generator);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block source,
                                             BlockPos sourcePos, boolean moved) {
        super.neighborChanged(state, level, pos, source, sourcePos, moved);
        if (level instanceof ServerLevel serverLevel) RailPowerService.markDirty(serverLevel);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level instanceof ServerLevel ? createTickerHelper(type, ModBlockEntities.POWER_GENERATOR,
                (tickLevel, pos, tickState, generator) -> {
                    if (tickLevel instanceof ServerLevel serverLevel) {
                        PowerGeneratorBlockEntity.serverTick(serverLevel, pos, tickState, generator);
                    }
                }) : null;
    }
}
