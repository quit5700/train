package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.block.entity.TrainDepotBlockEntity;
import cn.quit5700.fabrictrain.data.DeviceType;
import cn.quit5700.fabrictrain.data.RailwayDataManager;
import cn.quit5700.fabrictrain.registry.ModBlocks;
import cn.quit5700.fabrictrain.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public final class DeviceInteractionService {
    private DeviceInteractionService() {}

    public static InteractionResult interact(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || player.getItemInHand(hand).is(ModItems.LINE_SETTER)) {
            return InteractionResult.PASS;
        }
        var state = level.getBlockState(hit.getBlockPos());
        if (!state.is(ModBlocks.TRAIN_DEPOT)) return InteractionResult.PASS;
        if (level.isClientSide()) {
            if (DepotSettingsGesture.tryOpen(hit.getBlockPos())) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        ServerPlayer serverPlayer = (ServerPlayer) player;
        RailwayDataManager.register(serverLevel, hit.getBlockPos(), DeviceType.DEPOT);
        if (level.getBlockEntity(hit.getBlockPos()) instanceof TrainDepotBlockEntity depot) {
            if(player.isShiftKeyDown())return InteractionResult.PASS;
            serverPlayer.openMenu(depot);
        }
        return InteractionResult.SUCCESS;
    }
}
