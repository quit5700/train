package cn.quit5700.fabrictrain.depot;

import cn.quit5700.fabrictrain.block.entity.TrainDepotBlockEntity;
import cn.quit5700.fabrictrain.entity.CargoCarEntity;
import cn.quit5700.fabrictrain.entity.LocomotiveEntity;
import cn.quit5700.fabrictrain.entity.PassengerCarEntity;
import cn.quit5700.fabrictrain.registry.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import cn.quit5700.fabrictrain.data.RailwayDataManager;
import net.minecraft.world.Container;
import cn.quit5700.fabrictrain.cargo.CargoTransferService;

import java.util.ArrayList;
import java.util.List;

public final class DepotRecoveryService {
    private DepotRecoveryService() {}

    public static boolean recover(List<Entity> cars, TrainDepotBlockEntity depot) {
        List<Container> containers = new ArrayList<>(); containers.add(depot);
        if (depot.getLevel() instanceof ServerLevel level)
            containers.addAll(CargoTransferService.adjacentContainers(level, depot.getBlockPos()));
        List<ItemStack> simulated = new ArrayList<>();
        for (Container container : containers) for (int slot=0;slot<container.getContainerSize();slot++)
            simulated.add(container.getItem(slot).copy());
        List<ItemStack> returning = new ArrayList<>();
        for (Entity entity : cars) {
            if (entity instanceof CargoCarEntity cargo) {
                for (int slot = 0; slot < cargo.getContainerSize(); slot++) {
                    if (!cargo.getItem(slot).isEmpty()) returning.add(cargo.getItem(slot).copy());
                }
                returning.add(new ItemStack(ModItems.CARGO_CAR));
            } else if (entity instanceof PassengerCarEntity) {
                returning.add(new ItemStack(ModItems.PASSENGER_CAR));
            } else if (entity instanceof LocomotiveEntity) {
                returning.add(new ItemStack(ModItems.LOCOMOTIVE));
            }
        }
        for (ItemStack stack : returning) if (!insert(simulated, stack)) return false;
        int flat=0;for(Container container:containers){for(int slot=0;slot<container.getContainerSize();slot++)container.setItem(slot,simulated.get(flat++));container.setChanged();}
        cars.stream().filter(LocomotiveEntity.class::isInstance).map(LocomotiveEntity.class::cast)
                .findFirst().ifPresent(locomotive -> {
                    if (locomotive.level() instanceof ServerLevel level) {
                        cn.quit5700.fabrictrain.train.TrainControllerManager.retireTrainResources(
                                level, locomotive.trainLink().trainId());
                    }
                });
        cars.forEach(Entity::discard);
        return true;
    }

    public static boolean recoverItems(ServerLevel level, TrainDepotBlockEntity depot, List<ItemStack> returning) {
        List<Container> containers = new ArrayList<>(); containers.add(depot);
        containers.addAll(CargoTransferService.adjacentContainers(level, depot.getBlockPos()));
        List<ItemStack> simulated = new ArrayList<>();
        for (Container container : containers) for (int slot = 0; slot < container.getContainerSize(); slot++)
            simulated.add(container.getItem(slot).copy());
        for (ItemStack stack : returning) if (!insert(simulated, stack.copy())) return false;
        int flat = 0;
        for (Container container : containers) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) container.setItem(slot, simulated.get(flat++));
            container.setChanged();
        }
        return true;
    }

    private static boolean insert(List<ItemStack> inventory, ItemStack source) {
        for (ItemStack target : inventory) {
            if (source.isEmpty()) return true;
            if (target.isEmpty() || !ItemStack.isSameItemSameTags(source, target)) continue;
            int room = target.getMaxStackSize() - target.getCount();
            int moved = Math.min(room, source.getCount());
            target.grow(moved);
            source.shrink(moved);
        }
        for (int slot = 0; slot < inventory.size() && !source.isEmpty(); slot++) {
            if (!inventory.get(slot).isEmpty()) continue;
            int moved = Math.min(source.getMaxStackSize(), source.getCount());
            inventory.set(slot, source.copyWithCount(moved));
            source.shrink(moved);
        }
        return source.isEmpty();
    }
}
