package cn.quit5700.fabrictrain.cargo;

import cn.quit5700.fabrictrain.data.DeviceType;
import cn.quit5700.fabrictrain.data.RailwayDataManager;
import cn.quit5700.fabrictrain.data.RailwayDevice;
import cn.quit5700.fabrictrain.entity.CargoCarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class DestinationUnloadService {
    private DestinationUnloadService() {}

    public static void tick(MinecraftServer server){
        for(ServerLevel level:server.getAllLevels()){
            var data=RailwayDataManager.state(level);
            String dimension=RailwayDataManager.dimension(level);
            for(var entity:level.getAllEntities()){
                if(!(entity instanceof CargoCarEntity cargo)||cargo.isEmpty())continue;
                long destination = CargoUnloadRules.effectiveDestination(cargo.destinationStationId(),
                        cargo.trainLink().destinationStationId());
                if (destination <= 0L) continue;
                var station=data.device(destination);
                if(station==null||station.type()!=DeviceType.STATION||!station.dimension().equals(dimension))continue;
                BlockPos stationPos=new BlockPos(station.x(),station.y(),station.z());
                RailwayDevice currentStation = StationPresenceService.findStationAt(level, data, cargo);
                if (currentStation == null || currentStation.id() != station.id()) continue;
                CargoTransferService.unloadAtStation(level,stationPos,cargo);
            }
        }
    }
}
