package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.route.TrackPoint;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.UUID;

public record OfflineTrainState(UUID trainId, long lineId, TrainClass trainClass,
                                double configuredSpeedLimitKmh, int pathIndex, double segmentProgress,
                                double speed, boolean loop, int loopStartIndex, int nextNodeIndex,
                                long lastDeviceId, long destinationStationId, long dwellUntilGameTime, long lastGameTime,
                                List<TrackPoint> path, List<OfflineCarState> cars) {
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    private static final Codec<TrackPoint> TRACK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(TrackPoint::x),
            Codec.INT.fieldOf("y").forGetter(TrackPoint::y),
            Codec.INT.fieldOf("z").forGetter(TrackPoint::z)
    ).apply(instance, TrackPoint::new));
    private static final Codec<RouteState> ROUTE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("path_index").forGetter(RouteState::pathIndex),
            Codec.DOUBLE.fieldOf("segment_progress").forGetter(RouteState::segmentProgress),
            Codec.DOUBLE.fieldOf("speed").forGetter(RouteState::speed),
            Codec.BOOL.optionalFieldOf("loop", false).forGetter(RouteState::loop),
            Codec.INT.optionalFieldOf("loop_start_index", 0).forGetter(RouteState::loopStartIndex),
            Codec.INT.optionalFieldOf("next_node_index", 1).forGetter(RouteState::nextNodeIndex),
            Codec.LONG.optionalFieldOf("last_device_id", 0L).forGetter(RouteState::lastDeviceId),
            Codec.LONG.optionalFieldOf("destination_station_id", 0L).forGetter(RouteState::destinationStationId),
            Codec.LONG.optionalFieldOf("dwell_until", 0L).forGetter(RouteState::dwellUntilGameTime),
            Codec.LONG.fieldOf("last_game_time").forGetter(RouteState::lastGameTime),
            TRACK_CODEC.listOf().fieldOf("path").forGetter(RouteState::path)
    ).apply(instance, RouteState::new));
    public static final Codec<OfflineTrainState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUID_CODEC.fieldOf("train_id").forGetter(OfflineTrainState::trainId),
            Codec.LONG.fieldOf("line_id").forGetter(OfflineTrainState::lineId),
            Codec.STRING.xmap(TrainClass::valueOf, TrainClass::name).fieldOf("train_class").forGetter(OfflineTrainState::trainClass),
            Codec.DOUBLE.optionalFieldOf("configured_speed_limit_kmh", 0D).forGetter(OfflineTrainState::configuredSpeedLimitKmh),
            ROUTE_CODEC.fieldOf("route").forGetter(OfflineTrainState::routeState),
            OfflineCarState.CODEC.listOf().fieldOf("cars").forGetter(OfflineTrainState::cars)
    ).apply(instance, (trainId, lineId, trainClass, configuredSpeed, route, cars) -> new OfflineTrainState(
            trainId, lineId, trainClass, configuredSpeed, route.pathIndex(), route.segmentProgress(), route.speed(),
            route.loop(), route.loopStartIndex(), route.nextNodeIndex(), route.lastDeviceId(),
            route.destinationStationId(), route.dwellUntilGameTime(), route.lastGameTime(), route.path(), cars)));

    public OfflineTrainState {
        path = List.copyOf(path);
        cars = List.copyOf(cars);
        pathIndex = path.isEmpty() ? 0 : Math.clamp(pathIndex, 0, path.size() - 1);
        loopStartIndex = path.size() < 2 ? 0 : Math.clamp(loopStartIndex, 0, path.size() - 2);
        nextNodeIndex = Math.max(1, nextNodeIndex);
    }

    private RouteState routeState() {
        return new RouteState(pathIndex, segmentProgress, speed, loop, loopStartIndex, nextNodeIndex,
                lastDeviceId, destinationStationId, dwellUntilGameTime, lastGameTime, path);
    }
    private record RouteState(int pathIndex, double segmentProgress, double speed, boolean loop,
                              int loopStartIndex, int nextNodeIndex, long lastDeviceId,
                              long destinationStationId, long dwellUntilGameTime, long lastGameTime,
                              List<TrackPoint> path) {}
}
