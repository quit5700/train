package cn.quit5700.fabrictrain.cargo;

import cn.quit5700.fabrictrain.data.DeviceType;
import cn.quit5700.fabrictrain.data.RailwayDevice;
import cn.quit5700.fabrictrain.data.RailwayLine;
import cn.quit5700.fabrictrain.data.RailwaySavedData;
import cn.quit5700.fabrictrain.train.TrainLink;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class StationSelectionService {
    private StationSelectionService() {}

    public static List<RailwayDevice> availableStations(RailwaySavedData data, TrainLink link) {
        if (link.controlMode().playerDriveAllowed()) {
            return data.devices().values().stream().filter(device -> device.type() == DeviceType.STATION)
                    .sorted(java.util.Comparator.comparingLong(RailwayDevice::id)).toList();
        }
        RailwayLine line = data.line(link.lineId());
        if (line == null) return List.of();
        return line.nodeIds().stream().distinct().map(data::device).filter(Objects::nonNull)
                .filter(device -> device.type() == DeviceType.STATION).toList();
    }

    public static List<RailwayDevice> depotStations(net.minecraft.server.level.ServerLevel level,
            RailwaySavedData data, long lineId) {
        RailwayLine line = data.line(lineId);
        var candidates = line == null ? data.devices().values().stream()
                .sorted(java.util.Comparator.comparingLong(RailwayDevice::id))
                : line.nodeIds().stream().distinct().map(data::device);
        return candidates.filter(Objects::nonNull).filter(d -> d.type() == DeviceType.STATION)
                .filter(d -> hasContainer(level, d)).toList();
    }

    public static boolean canSelect(RailwaySavedData data, TrainLink link, long stationId) {
        RailwayDevice station = data.device(stationId);
        RailwayLine line = data.line(link.lineId());
        Set<Long> lineStations = line == null ? Set.of() : line.nodeIds().stream()
                .filter(id -> data.device(id) != null && data.device(id).type() == DeviceType.STATION)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return StationSelectionRules.canSelect(link.controlMode(),link.lineId(),stationId,
                station != null && station.type() == DeviceType.STATION,lineStations);
    }

    public static List<RailwayDevice> availableStations(net.minecraft.server.level.ServerLevel level, RailwaySavedData data, TrainLink link) {
        return availableStations(data, link).stream().filter(d -> hasContainer(level,d)).toList();
    }
    public static boolean canSelect(net.minecraft.server.level.ServerLevel level, RailwaySavedData data, TrainLink link,long stationId) {
        return stationId==0L || canSelect(data,link,stationId)&&hasContainer(level,data.device(stationId));
    }
    public static void refreshAt(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos pos){
        var data=cn.quit5700.fabrictrain.data.RailwayDataManager.state(level);var d=data.find(cn.quit5700.fabrictrain.data.RailwayDataManager.dimension(level),pos.getX(),pos.getY(),pos.getZ());if(d!=null&&d.type()==DeviceType.STATION)hasContainer(level,d);
    }
    private static boolean hasContainer(net.minecraft.server.level.ServerLevel level,RailwayDevice d) {
        if(d==null||!d.dimension().equals(cn.quit5700.fabrictrain.data.RailwayDataManager.dimension(level)))return false;
        var data=cn.quit5700.fabrictrain.data.RailwayDataManager.state(level);
        var pos=new net.minecraft.core.BlockPos(d.x(),d.y(),d.z());boolean loaded=level.hasChunkAt(pos);
        for(var side:net.minecraft.core.Direction.values())loaded&=level.hasChunkAt(pos.relative(side));
        if (loaded) cn.quit5700.fabrictrain.offline.InfrastructureService.captureStationStock(level, pos);
        if(loaded)data.stationContainer(d.id(),cn.quit5700.fabrictrain.data.LoadedRailWorld.state(level,pos).is(cn.quit5700.fabrictrain.registry.ModBlocks.STATION_MARKER)&&CargoTransferService.hasAdjacentContainer(level,pos));
        return Boolean.TRUE.equals(data.stationContainer(d.id()));
    }
    private static boolean knownInvalid(net.minecraft.server.level.ServerLevel level,RailwaySavedData data,TrainLink link,long id){
        if(id==0)return false;if(!canSelect(data,link,id))return true;
        hasContainer(level,data.device(id));return Boolean.FALSE.equals(data.stationContainer(id));
    }
    public static void tick(net.minecraft.server.MinecraftServer server) {
        if(server.getTickCount()%10!=0)return;
        for(var level:server.getAllLevels()) {
            var data=cn.quit5700.fabrictrain.data.RailwayDataManager.state(level);
            boolean initialized=false;
            for(var station:data.devices().values())if(station.type()==DeviceType.STATION&&station.dimension().equals(cn.quit5700.fabrictrain.data.RailwayDataManager.dimension(level))){
                if(data.stationContainer(station.id())==null&&!initialized){
                    var pos=new net.minecraft.core.BlockPos(station.x(),station.y(),station.z());level.getChunkAt(pos);for(var side:net.minecraft.core.Direction.values())level.getChunkAt(pos.relative(side));initialized=true;
                }
                hasContainer(level,station);
            }
            for(var e:level.getAllEntities())if(e instanceof cn.quit5700.fabrictrain.train.LinkedTrainCar car){
                if(knownInvalid(level,data,car.trainLink(),car.trainLink().destinationStationId()))car.trainLink().setDestinationStationId(0L);
                if(e instanceof cn.quit5700.fabrictrain.entity.CargoCarEntity cargo&&knownInvalid(level,data,car.trainLink(),cargo.destinationStationId()))cargo.setDestinationStationId(0L);
            }
        }
    }
}
