package cn.quit5700.fabrictrain.item;

import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import cn.quit5700.fabrictrain.route.WorldLineValidator;
import cn.quit5700.fabrictrain.network.LineSettingsNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import cn.quit5700.fabrictrain.network.RailwayManagerNetworking;

public final class LineSetterItem extends Item {
    private static final Map<UUID, Long> EDITING_LINES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SELECTED_LINES = new ConcurrentHashMap<>();

    public LineSetterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            cn.quit5700.fabrictrain.network.DepotRemoteAccessSessions.authorizeManager(
                    serverPlayer.getUUID(), System.nanoTime());
            RailwayManagerNetworking.open(serverPlayer);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if(context.getLevel() instanceof ServerLevel level&&context.getPlayer() instanceof ServerPlayer player) {
            cn.quit5700.fabrictrain.network.DepotRemoteAccessSessions.authorizeManager(
                    player.getUUID(), System.nanoTime());
            RailwayManagerNetworking.open(player,selectedLine(player.getUUID()));
        }
        return InteractionResult.SUCCESS;
    }

    private static DeviceType deviceType(BlockState state) {
        if (state.is(ModBlocks.TRAIN_DEPOT)) return DeviceType.DEPOT;
        if (state.is(ModBlocks.STATION_MARKER)) return DeviceType.STATION;
        if (state.is(ModBlocks.SWITCH_RAIL)) return DeviceType.SWITCH;
        return null;
    }

    public static void clearSessions() {
        EDITING_LINES.clear();
        SELECTED_LINES.clear();
    }

    public static long selectedLine(UUID playerId) {
        return SELECTED_LINES.getOrDefault(playerId, 0L);
    }

    public static void beginEditing(UUID playerId, long lineId) {
        EDITING_LINES.put(playerId, lineId);
        SELECTED_LINES.put(playerId, lineId);
    }
    public static void select(UUID playerId,long lineId){if(lineId>0)SELECTED_LINES.put(playerId,lineId);}

    private static long nextLineId(RailwaySavedData data, long selected) {
        java.util.List<Long> ids = data.lines().keySet().stream().sorted().toList();
        int current = ids.indexOf(selected);
        return ids.get((current + 1 + ids.size()) % ids.size());
    }
}
