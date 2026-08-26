package cn.quit5700.fabrictrain.data;

import cn.quit5700.fabrictrain.block.entity.TrainDepotBlockEntity;
import cn.quit5700.fabrictrain.route.RuntimePathCache;
import cn.quit5700.fabrictrain.train.TrainControllerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class LineDeletionService {
    private LineDeletionService() {}

    public static boolean delete(MinecraftServer server, ServerLevel sourceLevel, long lineId) {
        RailwaySavedData data = RailwayDataManager.state(sourceLevel);
        RailwayLine line = data.line(lineId);
        if (line == null) return false;
        TrainControllerManager.handleDeletedLine(server, lineId, data.recoveryDepotId(lineId));
        for (RailwayDevice device : data.devices().values()) {
            if (device.type() != DeviceType.DEPOT) continue;
            for (ServerLevel level : server.getAllLevels()) {
                if (!device.dimension().equals(RailwayDataManager.dimension(level))) continue;
                BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
                if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof TrainDepotBlockEntity depot
                        && depot.selectedLineId() == lineId) {
                    depot.setSelectedLineId(0L);
                    depot.setAutomaticDispatchEnabled(false);
                }
            }
        }
        RuntimePathCache.remove(lineId);
        return data.deleteLine(lineId);
    }
}
