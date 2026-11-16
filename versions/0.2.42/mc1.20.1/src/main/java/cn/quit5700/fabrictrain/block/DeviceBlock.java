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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

public class DeviceBlock extends Block {
    private final DeviceType type;

    public DeviceBlock(Properties properties, DeviceType type) {
        super(properties);
        this.type = type;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,net.minecraft.world.item.ItemStack stack){super.setPlacedBy(level,pos,state,placer,stack);if (level instanceof ServerLevel serverLevel) {
            var device = RailwayDataManager.register(serverLevel,pos,type);
            if (type == DeviceType.FREIGHT_YARD) cn.quit5700.fabrictrain.freight.FreightYardService.yard(serverLevel, device);
        }}

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            if (type == DeviceType.FREIGHT_YARD) cn.quit5700.fabrictrain.freight.FreightYardService.drop(serverLevel, pos);
            RailwayDataManager.remove(serverLevel, pos);
        }
        super.playerWillDestroy(level, pos, state, player);
    }
@Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block source,
                                             BlockPos sourcePos, boolean moved) {
        super.neighborChanged(state,level,pos,source,sourcePos,moved);
        if(level instanceof ServerLevel server)cn.quit5700.fabrictrain.cargo.StationSelectionService.refreshAt(server,pos);
    }
}
