package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.block.entity.TrainDepotBlockEntity;
import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.depot.DepotRecoveryService;
import cn.quit5700.fabrictrain.entity.LocomotiveEntity;
import cn.quit5700.fabrictrain.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import cn.quit5700.fabrictrain.entity.CargoCarEntity;
import cn.quit5700.fabrictrain.entity.PassengerCarEntity;
import cn.quit5700.fabrictrain.registry.ModItems;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;

public final class DerailedTrainRecoveryService {
    private DerailedTrainRecoveryService() {}

    public static int recoverLoaded(ServerLevel level, RailwayLine line, RailwaySavedData data) {
        RailwayDevice depotDevice = data.device(data.recoveryDepotId(line.id()));
        if (depotDevice == null || !depotDevice.dimension().equals(RailwayDataManager.dimension(level))) return 0;
        BlockPos depotPos = new BlockPos(depotDevice.x(), depotDevice.y(), depotDevice.z());
        if (!level.hasChunkAt(depotPos) || !(level.getBlockEntity(depotPos) instanceof TrainDepotBlockEntity depot)) return 0;
        Map<UUID, List<Entity>> trains = new LinkedHashMap<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LinkedTrainCar linked && linked.trainLink().lineId() == line.id()) {
                trains.computeIfAbsent(linked.trainLink().trainId(), ignored -> new ArrayList<>()).add(entity);
            }
        }
        int recovered = 0;
        for (List<Entity> cars : trains.values()) {
            LocomotiveEntity locomotive = cars.stream().filter(LocomotiveEntity.class::isInstance)
                    .map(LocomotiveEntity.class::cast).findFirst().orElse(null);
            if (locomotive == null || isValidTrack(level.getBlockState(cn.quit5700.fabrictrain.compat.MinecartCompat.currentRailPos(locomotive)))) continue;
            if (DepotRecoveryService.recover(cars, depot)) recovered++;
        }
        return recovered;
    }

    private static boolean isValidTrack(BlockState state) {
        return state.is(ModBlocks.SIGNAL_RAIL) || state.is(ModBlocks.SWITCH_RAIL);
    }

    public static void dropAsItems(ServerLevel level, List<Entity> cars) {
        UUID trainId = null;
        for (Entity entity : cars) {
            if (entity instanceof LinkedTrainCar linked) trainId = linked.trainLink().trainId();
            entity.ejectPassengers();
            if (entity instanceof CargoCarEntity cargo) {
                Containers.dropContents(level, cargo.blockPosition(), cargo);
                entity.spawnAtLocation(new ItemStack(ModItems.CARGO_CAR), 0.0F);
            } else if (entity instanceof PassengerCarEntity) {
                entity.spawnAtLocation(new ItemStack(ModItems.PASSENGER_CAR), 0.0F);
            } else if (entity instanceof LocomotiveEntity) {
                entity.spawnAtLocation(new ItemStack(ModItems.LOCOMOTIVE), 0.0F);
            }
            entity.discard();
        }
        if (trainId != null) {
            TrainControllerManager.retireTrainResources(level, trainId);
        }
    }
}

