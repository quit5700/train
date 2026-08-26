package cn.quit5700.fabrictrain.cargo;

import cn.quit5700.fabrictrain.entity.CargoCarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public final class CargoTransferService {
    private CargoTransferService() {
    }

    public static int unloadAtStation(ServerLevel level, BlockPos stationPos, CargoCarEntity cargo) {
        List<Container> destinations = adjacentContainers(level, stationPos);
        if (destinations.isEmpty()) return 0;
        int moved = 0;
        for (int sourceSlot = 0; sourceSlot < cargo.getContainerSize(); sourceSlot++) {
            ItemStack source = cargo.getItem(sourceSlot);
            if (source.isEmpty()) continue;
            int before = source.getCount();
            for (Container destination : destinations) insert(destination, source);
            moved += before - source.getCount();
            cargo.setItem(sourceSlot, source);
        }
        if (moved > 0) {
            cargo.setChanged();
            destinations.forEach(Container::setChanged);
        }
        return moved;
    }

    public static boolean hasAdjacentContainer(ServerLevel level, BlockPos stationPos) {
        return !adjacentContainers(level, stationPos).isEmpty();
    }

    public static int manualUnloadAtStation(ServerLevel level, BlockPos stationPos, CargoCarEntity cargo) {
        List<Container> destinations = adjacentContainers(level, stationPos);
        return manualUnload(destinations, cargo);
    }

    public static int manualUnload(List<? extends Container> destinations, Container cargo) {
        if (destinations.isEmpty()) return 0;
        int moved = 0;
        for (int sourceSlot = 0; sourceSlot < cargo.getContainerSize(); sourceSlot++) {
            ItemStack source = cargo.getItem(sourceSlot);
            if (source.isEmpty()) continue;
            int before = source.getCount();
            for (Container destination : destinations) insert(destination, source);
            moved += before - source.getCount();
            cargo.setItem(sourceSlot, source);
        }
        if (moved > 0) { cargo.setChanged(); destinations.forEach(Container::setChanged); }
        return moved;
    }

    public static List<ItemStack> deliverStacks(ServerLevel level, BlockPos stationPos, List<ItemStack> stacks) {
        List<Container> destinations = adjacentContainers(level, stationPos);
        if (destinations.isEmpty()) return stacks.stream().map(ItemStack::copy).toList();
        ArrayList<ItemStack> remaining = new ArrayList<>();
        for (ItemStack original : stacks) {
            ItemStack source = original.copy();
            for (Container destination : destinations) insert(destination, source);
            if (!source.isEmpty()) remaining.add(source);
        }
        destinations.forEach(Container::setChanged);
        return remaining;
    }

    public static List<Container> adjacentContainers(ServerLevel level, BlockPos stationPos) {
        ArrayList<Container> containers = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(stationPos.relative(direction)) instanceof Container container) containers.add(container);
        }
        return containers;
    }

    private static void insert(Container destination, ItemStack source) {
        for (int slot = 0; slot < destination.getContainerSize() && !source.isEmpty(); slot++) {
            ItemStack target = destination.getItem(slot);
            if (target.isEmpty()) continue;
            int capacity = destination.getMaxStackSize(target) - target.getCount();
            int amount = CargoSlotRules.movable(false,
                    ItemStack.isSameItemSameComponents(source, target), true, source.getCount(), capacity);
            if (amount <= 0) continue;
            target.grow(amount);
            source.shrink(amount);
            destination.setItem(slot, target);
        }
        for (int slot = 0; slot < destination.getContainerSize() && !source.isEmpty(); slot++) {
            if (!destination.getItem(slot).isEmpty()) continue;
            int amount = CargoSlotRules.movable(true, false, destination.canPlaceItem(slot, source),
                    source.getCount(), destination.getMaxStackSize(source));
            if (amount <= 0) continue;
            destination.setItem(slot, source.copyWithCount(amount));
            source.shrink(amount);
        }
    }
}
