package cn.quit5700.fabrictrain.route;

import cn.quit5700.fabrictrain.data.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BaseRailBlock;
import cn.quit5700.fabrictrain.registry.ModBlocks;
import cn.quit5700.fabrictrain.power.RailPowerService;
import cn.quit5700.fabrictrain.power.PowerSectionCache;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import cn.quit5700.fabrictrain.depot.DepotDirectionRules;
import cn.quit5700.fabrictrain.block.DeviceRailBlock;
import net.minecraft.core.Direction;

public final class WorldLineValidator {
    public static final int MAXIMUM_VISITED_TRACKS_PER_SEGMENT = 20_000;

    private WorldLineValidator() {
    }

    public static boolean validate(ServerLevel level, RailwayLine line, RailwaySavedData data) {
        return validateDetailed(level, line, data).valid();
    }

    public static LineValidationResult validateDetailed(ServerLevel level, RailwayLine line, RailwaySavedData data) {
        if (line.nodeIds().size() < 2) {
            RuntimePathCache.remove(line.id());
            return new LineValidationResult(false, -1, -1, LineValidationFailure.INVALID_NODE);
        }
        ArrayList<TrackPoint> complete = new ArrayList<>();
        for (int i = 1; i < line.nodeIds().size(); i++) {
            RailwayDevice from = data.device(line.nodeIds().get(i - 1));
            RailwayDevice to = data.device(line.nodeIds().get(i));
            if (from == null || to == null || !from.dimension().equals(to.dimension())) {
                RuntimePathCache.remove(line.id());
                return new LineValidationResult(false, i - 1, i, LineValidationFailure.INVALID_NODE);
            }
            TrackPoint start = trackPoint(level, line, i - 1, from, data);
            TrackPoint destination = trackPoint(level, line, i, to, data);
            if (start == null || destination == null) {
                RuntimePathCache.remove(line.id());
                return new LineValidationResult(false, i - 1, i, LineValidationFailure.STATION_STOP_UNBOUND);
            }
            if (!isRail(level, start) || !isRail(level, destination)) {
                RuntimePathCache.remove(line.id());
                return new LineValidationResult(false, i - 1, i, LineValidationFailure.TRACK_BROKEN);
            }
            int sourceChoice = line.switchDirection(i - 1) >= 0
                    ? line.switchDirection(i - 1) : from.switchShapeOrdinal();
            TrackPoint incoming = complete.size() >= 2 ? complete.get(complete.size() - 2) : null;
            PathScanResult result = TrackPathScanner.scanDirected(start, destination,
                    MAXIMUM_VISITED_TRACKS_PER_SEGMENT,
                    incoming,
                    (previous, point) -> segmentNeighbours(level, point, start, from, sourceChoice));
            if (result.status() != PathScanResult.Status.FOUND) {
                RuntimePathCache.remove(line.id());
                return new LineValidationResult(false, i - 1, i, LineValidationFailure.TRACK_BROKEN);
            }
            if (!complete.isEmpty()) complete.remove(complete.size() - 1);
            complete.addAll(result.path());
        }
        RuntimePathCache.put(line.id(), complete);
        PowerSectionCache.put(level, line.id(), complete);
        return new LineValidationResult(true, -1, -1, LineValidationFailure.NONE);
    }

    public enum LineValidationFailure { NONE, INVALID_NODE, STATION_STOP_UNBOUND, TRACK_BROKEN }
    public record LineValidationResult(boolean valid, int fromIndex, int toIndex, LineValidationFailure failure) {}

    public static ImmediateDispatchStatus validateForImmediateDispatch(ServerLevel level, BlockPos depotPos,
                                                                       RailwayLine line, RailwaySavedData data) {
        return validateForImmediateDispatchDetailed(level, depotPos, line, data).status();
    }

    public static ImmediateDispatchResult validateForImmediateDispatchDetailed(ServerLevel level, BlockPos depotPos,
                                                                                RailwayLine line, RailwaySavedData data) {
        if (line.nodeIds().isEmpty()) return new ImmediateDispatchResult(
                ImmediateDispatchStatus.FIRST_NODE_NOT_FOUND, -1, -1);
        RailwayDevice first = data.device(line.nodeIds().get(0));
        if (first == null || !first.dimension().equals(RailwayDataManager.dimension(level)))
            return new ImmediateDispatchResult(ImmediateDispatchStatus.FIRST_NODE_NOT_FOUND, -1, -1);
        LineValidationResult validation = validateDetailed(level, line, data);
        if (!validation.valid()) return new ImmediateDispatchResult(
                validation.failure() == LineValidationFailure.STATION_STOP_UNBOUND
                        ? ImmediateDispatchStatus.STATION_STOP_UNBOUND : ImmediateDispatchStatus.TRACK_BROKEN,
                validation.fromIndex(), validation.toIndex());
        line = data.line(line.id());
        DispatchAccess access = validateDispatchAccess(level, depotPos, line, data);
        if (access.status() == DispatchAccessStatus.NO_PATH) return new ImmediateDispatchResult(
                ImmediateDispatchStatus.FIRST_NODE_NOT_FOUND, -1, -1);
        if (access.status() == DispatchAccessStatus.UNPOWERED) return new ImmediateDispatchResult(
                ImmediateDispatchStatus.UNPOWERED, -1, -1);
        PowerSectionCache.refreshLoaded(level, line.id());
        boolean powered = PowerSectionCache.allPowered(line.id());
        return new ImmediateDispatchResult(powered ? ImmediateDispatchStatus.READY
                : ImmediateDispatchStatus.UNPOWERED, -1, -1);
    }

    public enum ImmediateDispatchStatus { READY, FIRST_NODE_NOT_FOUND, STATION_STOP_UNBOUND, TRACK_BROKEN, UNPOWERED }
    public record ImmediateDispatchResult(ImmediateDispatchStatus status, int fromIndex, int toIndex) {}

    public static DispatchAccess validateDispatchAccess(ServerLevel level, RailwayLine line,
                                                        RailwaySavedData data) {
        RailwayDevice depot = data.device(line.depotId());
        if (depot == null || depot.type() != DeviceType.DEPOT
                || !depot.dimension().equals(RailwayDataManager.dimension(level))) {
            return new DispatchAccess(DispatchAccessStatus.NO_PATH, List.of());
        }
        return validateDispatchAccess(level, new BlockPos(depot.x(), depot.y(), depot.z()), line, data);
    }

    public static DispatchAccess validateDispatchAccess(ServerLevel level, BlockPos depotPos, RailwayLine line,
                                                        RailwaySavedData data) {
        RailwayLine currentLine = data.line(line.id());
        if (currentLine != null) line = currentLine;
        TrackPoint start = new TrackPoint(depotPos.getX(), depotPos.getY() + 1, depotPos.getZ());
        RailwayDevice firstNode = null;
        TrackPoint destination = null;
        for (int nodeIndex = 0; nodeIndex < line.nodeIds().size(); nodeIndex++) {
            RailwayDevice candidate = data.device(line.nodeIds().get(nodeIndex));
            if (candidate == null || !candidate.dimension().equals(RailwayDataManager.dimension(level))) continue;
            TrackPoint candidateTrack = trackPoint(level, line, nodeIndex, candidate, data);
            if (candidateTrack != null && !candidateTrack.equals(start)) {
                firstNode = candidate;
                destination = candidateTrack;
                break;
            }
        }
        if (firstNode == null || !isRail(level, start)) {
            return new DispatchAccess(DispatchAccessStatus.NO_PATH, List.of());
        }
        if (!isRail(level, destination)) return new DispatchAccess(DispatchAccessStatus.NO_PATH, List.of());
        BlockState startState = level.getBlockState(new BlockPos(start.x(), start.y(), start.z()));
        if (!startState.hasProperty(RailBlock.SHAPE)) return new DispatchAccess(DispatchAccessStatus.NO_PATH, List.of());
        List<TrackPoint> exits = RailConnectionResolver.connectedCandidates(start,
                startState.getValue(RailBlock.SHAPE), point -> isRail(level, point));
        TrackPoint first = exits.get(0);
        TrackPoint second = exits.get(exits.size() - 1);
        PathScanResult firstResult = scanBranch(level, start, first, destination);
        PathScanResult secondResult = scanBranch(level, start, second, destination);
        int chosen = DepotDirectionRules.frontIndex(distance(firstResult), distance(secondResult));
        if (chosen < 0) {
            return new DispatchAccess(DispatchAccessStatus.NO_PATH, List.of());
        }
        PathScanResult result = chosen == 0 ? firstResult : secondResult;
        ArrayList<TrackPoint> path = new ArrayList<>(); path.add(start); path.addAll(result.path());
        boolean powered = path.stream().allMatch(point -> RailPowerService.isPowered(level,
                new BlockPos(point.x(), point.y(), point.z())));
        return powered ? new DispatchAccess(DispatchAccessStatus.READY, path)
                : new DispatchAccess(DispatchAccessStatus.UNPOWERED, path);
    }

    private static int distance(PathScanResult result) {
        return result.status() == PathScanResult.Status.FOUND ? result.path().size() : -1;
    }

    private static PathScanResult scanBranch(ServerLevel level, TrackPoint origin, TrackPoint first,
                                             TrackPoint destination) {
        if (!isRail(level, first)) return new PathScanResult(PathScanResult.Status.NOT_FOUND, List.of());
        return TrackPathScanner.scan(first, destination, MAXIMUM_VISITED_TRACKS_PER_SEGMENT,
                point -> neighbours(level, point).stream().filter(candidate -> !candidate.equals(origin)).toList());
    }

    public enum DispatchAccessStatus { READY, NO_PATH, UNPOWERED }

    public record DispatchAccess(DispatchAccessStatus status, List<TrackPoint> path) {
        public DispatchAccess { path = List.copyOf(path); }
    }

    private static TrackPoint trackPoint(ServerLevel level, RailwayLine line, int nodeIndex,
                                         RailwayDevice device, RailwaySavedData data) {
        int y = device.type() == DeviceType.DEPOT ? device.y() + 1 : device.y();
        if (device.type() == DeviceType.STATION) {
            StopTrack stop = resolveStationStopTrack(level, line, nodeIndex, device, data);
            return stop == null ? null : new TrackPoint(stop.x(), stop.y(), stop.z());
        }
        return new TrackPoint(device.x(), y, device.z());
    }

    /**
     * A station marker is the support block below its stop rail. The exact stop is therefore always
     * the normal signal rail at the same X/Z and one block higher; nearby tracks never participate.
     */
    public static StopTrack resolveStationStopTrack(ServerLevel level, RailwayLine line, int nodeIndex,
                                                    RailwayDevice station, RailwaySavedData data) {
        StopTrack exact = StationStopTrackRules.directlyAbove(station.x(), station.y(), station.z());
        BlockPos stopPos = new BlockPos(exact.x(), exact.y(), exact.z());
        if (!level.getBlockState(stopPos).is(ModBlocks.SIGNAL_RAIL)) return null;
        RailwayLine current = data.line(line.id());
        if (current == null) return null;
        if (exact.equals(current.stationStopTrack(nodeIndex))) return exact;
        RailwayLine updated = data.setStationStopTrack(line.id(), nodeIndex, exact);
        return updated == null ? null : updated.stationStopTrack(nodeIndex);
    }

    private static boolean isRail(ServerLevel level, TrackPoint point) {
        BlockState state = level.getBlockState(new BlockPos(point.x(), point.y(), point.z()));
        return state.is(ModBlocks.SIGNAL_RAIL) || state.is(ModBlocks.SWITCH_RAIL);
    }

    private static Collection<TrackPoint> neighbours(ServerLevel level, TrackPoint point) {
        BlockState state = level.getBlockState(new BlockPos(point.x(), point.y(), point.z()));
        if (!state.hasProperty(RailBlock.SHAPE)) return List.of();
        return RailConnectionResolver.neighbours(point, state.getValue(RailBlock.SHAPE),
                candidate -> isRail(level, candidate));
    }

    private static Collection<TrackPoint> segmentNeighbours(ServerLevel level, TrackPoint point,
                                                            TrackPoint source, RailwayDevice sourceDevice,
                                                            int sourceChoice) {
        if (sourceDevice.type() != DeviceType.SWITCH || !point.equals(source)) return neighbours(level, point);
        BlockState state = level.getBlockState(new BlockPos(source.x(), source.y(), source.z()));
        if (!state.hasProperty(DeviceRailBlock.ENTRY_DIRECTION)) return List.of();
        Direction exit = SwitchRailRouting.exitFor(SwitchRailRouting.entryFor(sourceDevice, state), sourceChoice);
        TrackPoint next = step(level, source, exit);
        return isRail(level, next) ? List.of(next) : List.of();
    }

    private static TrackPoint step(ServerLevel level, TrackPoint origin, Direction direction) {
        BlockPos next = new BlockPos(origin.x(), origin.y(), origin.z()).relative(direction);
        TrackPoint candidate = new TrackPoint(next.getX(), next.getY(), next.getZ());
        if (!isRail(level, candidate)) {
            TrackPoint below = new TrackPoint(candidate.x(), candidate.y() - 1, candidate.z());
            TrackPoint above = new TrackPoint(candidate.x(), candidate.y() + 1, candidate.z());
            if (isRail(level, below)) return below;
            if (isRail(level, above)) return above;
        }
        return candidate;
    }
}

