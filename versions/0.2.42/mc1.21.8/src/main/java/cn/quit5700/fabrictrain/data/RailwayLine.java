package cn.quit5700.fabrictrain.data;

import java.util.List;
import java.util.Map;

public record RailwayLine(
        long id,
        String name,
        long depotId,
        List<Long> nodeIds,
        int departureIntervalSeconds,
        double speedLimitKmh,
        int maximumTrains,
        LineStatus status,
        Map<Integer, Integer> switchDirections,
        Map<Integer, StopTrack> stationStopTracks
) {
    public RailwayLine {
        nodeIds = List.copyOf(nodeIds);
        switchDirections = Map.copyOf(switchDirections);
        stationStopTracks = Map.copyOf(stationStopTracks);
        if (id <= 0 || depotId < 0 || departureIntervalSeconds < 0 || speedLimitKmh < 0 || maximumTrains < 1) {
            throw new IllegalArgumentException("Invalid railway line");
        }
    }

    public RailwayLine(long id, String name, long depotId, List<Long> nodeIds, int departureIntervalSeconds,
                       double speedLimitKmh, int maximumTrains, LineStatus status) {
        this(id, name, depotId, nodeIds, departureIntervalSeconds, speedLimitKmh, maximumTrains, status, Map.of(), Map.of());
    }

    public RailwayLine(long id, String name, long depotId, List<Long> nodeIds, int departureIntervalSeconds,
                       double speedLimitKmh, int maximumTrains, LineStatus status,
                       Map<Integer, Integer> switchDirections) {
        this(id, name, depotId, nodeIds, departureIntervalSeconds, speedLimitKmh, maximumTrains, status,
                switchDirections, Map.of());
    }

    public RailwayLine appendNode(long nodeId) {
        java.util.ArrayList<Long> copy = new java.util.ArrayList<>(nodeIds);
        copy.add(nodeId);
        return new RailwayLine(id, name, depotId, copy, departureIntervalSeconds,
                speedLimitKmh, maximumTrains, LineStatus.NEEDS_VALIDATION, switchDirections, stationStopTracks);
    }

    public int switchDirection(int nodeIndex) { return switchDirections.getOrDefault(nodeIndex, -1); }
    public StopTrack stationStopTrack(int nodeIndex) { return stationStopTracks.get(nodeIndex); }
}
