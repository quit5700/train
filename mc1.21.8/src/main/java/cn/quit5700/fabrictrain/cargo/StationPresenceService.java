package cn.quit5700.fabrictrain.cargo;

import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.entity.CargoCarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class StationPresenceService {
    private StationPresenceService() {}

    public static RailwayDevice findStationAt(ServerLevel level, RailwaySavedData data, CargoCarEntity cargo) {
        BlockPos railPos = cargo.getCurrentBlockPosOrRailBelow();
        for (RailwayLine line : data.lines().values()) {
            for (int index = 0; index < line.nodeIds().size(); index++) {
                RailwayDevice station = data.device(line.nodeIds().get(index));
                if (station == null || station.type() != DeviceType.STATION
                        || !station.dimension().equals(RailwayDataManager.dimension(level))) continue;
                // Presence checks must not mutate line data or auto-bind old stations. Migration is
                // performed only by line validation; here we consume an already persisted stop.
                StopTrack stop = line.stationStopTrack(index);
                if (stop != null && railPos.getX() == stop.x() && railPos.getY() == stop.y() && railPos.getZ() == stop.z())
                    return station;
            }
        }
        String dimension = RailwayDataManager.dimension(level);
        for (RailwayDevice station : data.devices().values()) {
            if (station.type() != DeviceType.STATION || !station.dimension().equals(dimension)) continue;
            Vec3 center = Vec3.atCenterOf(new BlockPos(station.x(), station.y(), station.z()));
            if (CargoDestinationRules.atStation(cargo.getX(), cargo.getY(), cargo.getZ(), center.x, center.y, center.z))
                return station;
        }
        return null;
    }
}
