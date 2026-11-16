package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.entity.LocomotiveEntity;
import cn.quit5700.fabrictrain.registry.ModItems;
import cn.quit5700.fabrictrain.item.LineSetterItem;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import cn.quit5700.fabrictrain.entity.CargoCarEntity;
import cn.quit5700.fabrictrain.entity.PassengerCarEntity;
import cn.quit5700.fabrictrain.data.*;
import net.minecraft.server.level.ServerLevel;
import java.util.List;
import cn.quit5700.fabrictrain.network.LegacyServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import cn.quit5700.fabrictrain.network.TrainClassNetworking;
import cn.quit5700.fabrictrain.network.CargoSettingsNetworking;
import cn.quit5700.fabrictrain.cargo.StationSelectionService;

public final class TrainInteractionEvents {
    private TrainInteractionEvents() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register(DeviceInteractionService::interact);
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
                ConnectorInteractionService.attack(player, hand, entity));
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
                player.getItemInHand(hand).is(ModItems.TRAIN_CONNECTOR)
                        ? InteractionResult.FAIL : InteractionResult.PASS);
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (entity instanceof LinkedTrainCar linked) TrainControllerManager.noteInteraction(linked);
            if (entity instanceof LinkedTrainCar && player.getItemInHand(hand).is(ModItems.TRAIN_CONNECTOR))
                return ConnectorInteractionService.rightClick(player, hand, entity);
            if (entity instanceof PassengerCarEntity && !player.isShiftKeyDown()
                    && !player.getItemInHand(hand).is(ModItems.LINE_SETTER)) {
                if (!level.isClientSide()) player.startRiding(entity);
                return InteractionResult.SUCCESS;
            }
            if (entity instanceof CargoCarEntity cargo && player.isShiftKeyDown() && !player.getItemInHand(hand).is(ModItems.LINE_SETTER)) {
                if (level instanceof ServerLevel serverLevel) {
                    RailwaySavedData data = RailwayDataManager.state(serverLevel);
                    List<CargoSettingsNetworking.Station> stations = StationSelectionService.availableStations((net.minecraft.server.level.ServerLevel) level,data,cargo.trainLink()).stream()
                            .map(device -> new CargoSettingsNetworking.Station(device.id(), device.name(), estimateEta(cargo, device))).toList();
                    LegacyServerPlayNetworking.send((ServerPlayer) player, new CargoSettingsNetworking.OpenPayload(
                            cargo.getId(), cargo.destinationStationId(), stations));
                }
                return InteractionResult.SUCCESS;
            }
            if (!(entity instanceof LocomotiveEntity locomotive)) {
                return InteractionResult.PASS;
            }
            if (player.isShiftKeyDown()&&level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                boolean hasCargo = false;
                for (var candidate : serverLevel.getAllEntities()) {
                    if (candidate instanceof CargoCarEntity cargo
                            && cargo.trainLink().trainId().equals(locomotive.trainLink().trainId())) {
                        hasCargo = true;
                        break;
                    }
                }
                RailwaySavedData speedData = RailwayDataManager.state(serverLevel);
                List<TrainClassNetworking.Station> stationOptions=StationSelectionService.availableStations((net.minecraft.server.level.ServerLevel) level,speedData,locomotive.trainLink()).stream().map(d->new TrainClassNetworking.Station(d.id(),d.name())).toList();
                TrainClassNetworking.authorizeSettings(serverPlayer);
                LegacyServerPlayNetworking.send(serverPlayer, new TrainClassNetworking.OpenPayload(
                        locomotive.getId(), locomotive.trainLink().trainClass().ordinal(), hasCargo,
                        speedData.classSpeedLimit(TrainClass.CARGO), speedData.classSpeedLimit(TrainClass.PASSENGER),
                        speedData.classSpeedLimit(TrainClass.EXPRESS),locomotive.trainLink().destinationStationId(),stationOptions));
            } else if (!level.isClientSide()) {
                player.startRiding(locomotive);
            }
            return InteractionResult.SUCCESS;
        });
    }

    private static int estimateEta(CargoCarEntity cargo, RailwayDevice station) {
        double distance = Math.sqrt(cargo.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(
                new net.minecraft.core.BlockPos(station.x(), station.y(), station.z()))));
        double blocksPerSecond = Math.max(1.0D, cargo.getDeltaMovement().horizontalDistance() * 20.0D);
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(distance / blocksPerSecond));
    }

    private static long nextStation(ServerLevel level, CargoCarEntity cargo) {
        RailwaySavedData data = RailwayDataManager.state(level);
        RailwayLine line = data.line(cargo.trainLink().lineId());
        if (line == null) return 0L;
        List<Long> stations = line.nodeIds().stream()
                .filter(id -> data.device(id) != null && data.device(id).type() == DeviceType.STATION).distinct().toList();
        if (stations.isEmpty()) return 0L;
        int current = stations.indexOf(cargo.destinationStationId());
        return stations.get((current + 1) % stations.size());
    }
}


