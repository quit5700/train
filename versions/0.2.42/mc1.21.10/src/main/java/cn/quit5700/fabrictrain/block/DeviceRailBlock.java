package cn.quit5700.fabrictrain.block;

import cn.quit5700.fabrictrain.data.DeviceType;
import cn.quit5700.fabrictrain.data.RailwayDataManager;
import cn.quit5700.fabrictrain.data.RailwayDevice;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;
import cn.quit5700.fabrictrain.network.DeviceSettingsNetworking;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RailBlock;
import cn.quit5700.fabrictrain.route.SwitchRailRouting;

public final class DeviceRailBlock extends SignalRailBlock {
    public static final IntegerProperty SWITCH_CHOICE = IntegerProperty.create("switch_choice", 0, 2);
    public static final EnumProperty<Direction> ENTRY_DIRECTION = EnumProperty.create(
            "entry_direction", Direction.class, Direction.Plane.HORIZONTAL);
    public static final EnumProperty<Direction> PLACEMENT_ENTRY = EnumProperty.create(
            "placement_entry", Direction.class, Direction.Plane.HORIZONTAL);
    private final DeviceType type;

    public DeviceRailBlock(Properties properties, DeviceType type) {
        super(properties);
        this.type = type;
        registerDefaultState(defaultBlockState().setValue(SWITCH_CHOICE, 1)
                .setValue(ENTRY_DIRECTION, Direction.NORTH).setValue(PLACEMENT_ENTRY, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SWITCH_CHOICE, ENTRY_DIRECTION, PLACEMENT_ENTRY);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                                      net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        Direction entry = placer == null ? Direction.NORTH : placer.getDirection().getOpposite();
        if (entry.getAxis() == Direction.Axis.Y) entry = Direction.NORTH;
        BlockState current = level.getBlockState(pos);
        int choice = current.getValue(SWITCH_CHOICE);
        level.setBlock(pos, current.setValue(ENTRY_DIRECTION, entry).setValue(PLACEMENT_ENTRY, entry)
                .setValue(RailBlock.SHAPE, SwitchRailRouting.shapeForEntry(entry, choice)), 3);
        if(level instanceof ServerLevel serverLevel) {
            RailwayDevice device=RailwayDataManager.register(serverLevel,pos,type);
            if(type==DeviceType.SWITCH)RailwayDataManager.state(serverLevel).updateSwitchEntry(device.id(),entry.ordinal());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            RailwayDataManager.remove(serverLevel, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
