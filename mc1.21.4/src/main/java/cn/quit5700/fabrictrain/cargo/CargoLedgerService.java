package cn.quit5700.fabrictrain.cargo;

import cn.quit5700.fabrictrain.data.RailwayDataManager;
import cn.quit5700.fabrictrain.data.RailwayDevice;
import cn.quit5700.fabrictrain.entity.CargoCarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CargoLedgerService {
    private CargoLedgerService() {}

    public static CargoLedgerSavedData state(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(CargoLedgerSavedData.TYPE, CargoLedgerSavedData.DATA_ID);
    }

    public static boolean escrow(ServerLevel level, CargoCarEntity cargo, long stationId, long dueGameTime) {
        CargoLedgerSavedData ledger = state(level);
        if (cargo.isEmpty() || ledger.containsSource(cargo.getUUID())) return false;
        ArrayList<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < cargo.getContainerSize(); slot++) {
            ItemStack stack = cargo.getItem(slot);
            if (!stack.isEmpty()) stacks.add(stack.copy());
        }
        if (stacks.isEmpty()) return false;
        ledger.add(new PendingDelivery(UUID.randomUUID(), cargo.getUUID(), stationId, dueGameTime, stacks));
        for (int slot = 0; slot < cargo.getContainerSize(); slot++) cargo.setItem(slot, ItemStack.EMPTY);
        cargo.setChanged();
        return true;
    }

    /** Records cargo that reached an unloaded station; delivery to containers happens when it loads. */
    public static void depositArrived(ServerLevel level, UUID sourceCarId, long stationId, List<ItemStack> stacks) {
        List<ItemStack> cargo = stacks.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList();
        if (stationId <= 0L || cargo.isEmpty()) return;
        state(level).add(new PendingDelivery(UUID.randomUUID(), sourceCarId, stationId,
                level.getGameTime(), cargo));
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        CargoLedgerSavedData ledger = state(server.overworld());
        for (PendingDelivery delivery : ledger.deliveries()) {
            if (server.overworld().getGameTime() < delivery.dueGameTime()) continue;
            RailwayDevice station = RailwayDataManager.state(server.overworld()).device(delivery.stationId());
            if (station == null) continue;
            ServerLevel destinationLevel = null;
            for (ServerLevel candidate : server.getAllLevels()) {
                if (RailwayDataManager.dimension(candidate).equals(station.dimension())) { destinationLevel = candidate; break; }
            }
            if (destinationLevel == null) continue;
            BlockPos stationPos = new BlockPos(station.x(), station.y(), station.z());
            if (!destinationLevel.hasChunkAt(stationPos)) continue;
            List<ItemStack> remaining = CargoTransferService.deliverStacks(destinationLevel, stationPos, delivery.stacks());
            ledger.replace(delivery, new PendingDelivery(delivery.shipmentId(), delivery.sourceCarId(),
                    delivery.stationId(), delivery.dueGameTime(), remaining));
        }
    }
}
