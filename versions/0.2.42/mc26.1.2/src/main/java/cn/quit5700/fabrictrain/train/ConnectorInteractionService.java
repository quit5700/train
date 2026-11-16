package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.item.TrainConnectorItem;
import cn.quit5700.fabrictrain.network.FormationNetworking;
import cn.quit5700.fabrictrain.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class ConnectorInteractionService {
    private ConnectorInteractionService() {}

    public static InteractionResult rightClick(Player player, InteractionHand hand, Entity entity) {
        if (!player.getItemInHand(hand).is(ModItems.TRAIN_CONNECTOR)) return InteractionResult.PASS;
        if (entity.level().isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            FormationNetworking.open((ServerPlayer) player, entity);
        }
        return InteractionResult.SUCCESS;
    }

    public static InteractionResult attack(Player player, InteractionHand hand, Entity entity) {
        if (!player.getItemInHand(hand).is(ModItems.TRAIN_CONNECTOR)
                || !(entity instanceof LinkedTrainCar)) return InteractionResult.PASS;
        if (entity.level().isClientSide()) return InteractionResult.SUCCESS;
        ServerPlayer serverPlayer = (ServerPlayer) player;
        var stack = player.getItemInHand(hand);
        var selectedId = TrainConnectorItem.selected(serverPlayer);
        if (selectedId == null) {
            TrainConnectorItem.select(serverPlayer, entity.getUUID(), stack);
            player.sendSystemMessage(Component.translatable("message.fabric_train_simulator.connector.selected"));
            return InteractionResult.SUCCESS;
        }
        Entity selected = ((ServerLevel) entity.level()).getEntity(selectedId);
        if (selected == null) {
            TrainConnectorItem.clear(serverPlayer, stack);
            player.sendSystemMessage(Component.translatable("message.fabric_train_simulator.connector.missing"));
            return InteractionResult.SUCCESS;
        }
        TrainFormationService.PairResult result = TrainFormationService.operatePair(selected, entity);
        TrainConnectorItem.clear(serverPlayer, stack);
        String message = switch (result) {
            case CONNECTED -> "message.fabric_train_simulator.connector.connected";
            case DISCONNECTED -> "message.fabric_train_simulator.connector.detached";
            case CANCELLED -> "message.fabric_train_simulator.connector.cancelled";
            case NOT_ADJACENT -> "message.fabric_train_simulator.connector.not_adjacent";
            case TOO_FAR -> "message.fabric_train_simulator.connector.too_far";
            case TOO_LONG -> "message.fabric_train_simulator.connector.too_long";
            case TWO_LOCOMOTIVES -> "message.fabric_train_simulator.connector.two_locomotives";
            case MIXED_ORIGIN -> "message.fabric_train_simulator.connector.mixed_origin";
            case NO_CONTINUOUS_TRACK -> "message.fabric_train_simulator.connector.no_continuous_track";
            case FAILED -> "message.fabric_train_simulator.connector.failed";
        };
        player.sendSystemMessage(Component.translatable(message));
        return InteractionResult.SUCCESS;
    }
}
