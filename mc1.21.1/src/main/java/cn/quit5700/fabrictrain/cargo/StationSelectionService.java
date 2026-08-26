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

    public static boolean canSelect(RailwaySavedData data, TrainLink link, long stationId) {
        RailwayDevice station = data.device(stationId);
        RailwayLine line = data.line(link.lineId());
        Set<Long> lineStations = line == null ? Set.of() : line.nodeIds().stream()
                .filter(id -> data.device(id) != null && data.device(id).type() == DeviceType.STATION)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return StationSelectionRules.canSelect(link.controlMode(),link.lineId(),stationId,
                station != null && station.type() == DeviceType.STATION,lineStations);
    }
}
