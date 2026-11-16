package cn.quit5700.fabrictrain.power;

import cn.quit5700.fabrictrain.route.TrackPoint;
import cn.quit5700.fabrictrain.offline.InfrastructureService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Runtime-only power cache. A route is divided into fixed 64-track sections. */
public final class PowerSectionCache {
    public static final int SECTION_LENGTH = 64;
    private static final Map<Long, java.lang.ref.WeakReference<ServerLevel>> LEVELS = new ConcurrentHashMap<>();
    private static final Map<Long, PowerSectionSnapshot> SNAPSHOTS = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Set<RoutePoint>> ROUTES_BY_RAIL = new ConcurrentHashMap<>();

    private PowerSectionCache() {}

    public static synchronized void put(ServerLevel level, long lineId, List<TrackPoint> path) {
        remove(lineId);
        LEVELS.put(lineId,new java.lang.ref.WeakReference<>(level));
        PowerSectionSnapshot snapshot = PowerSectionSnapshot.create(path, SECTION_LENGTH, point ->
                status(level, new BlockPos(point.x(), point.y(), point.z()), null));
        SNAPSHOTS.put(lineId, snapshot);
        for (int index = 0; index < path.size(); index++) {
            TrackPoint point = path.get(index);
            BlockPos pos = new BlockPos(point.x(), point.y(), point.z());
            ROUTES_BY_RAIL.computeIfAbsent(pos, ignored -> ConcurrentHashMap.newKeySet())
                    .add(new RoutePoint(lineId, index));
        }
    }

    /** Refreshes only route points whose rail power actually changed. */
    public static synchronized void refreshChanged(ServerLevel level, Collection<BlockPos> changed) {
        for (BlockPos pos : changed) {
            Set<RoutePoint> references = ROUTES_BY_RAIL.get(pos);
            if (references == null) continue;
            PowerSectionStatus state = status(level, pos, null);
            for (RoutePoint reference : references) {
                PowerSectionSnapshot snapshot = SNAPSHOTS.get(reference.lineId());
                if (snapshot != null) SNAPSHOTS.put(reference.lineId(),
                        snapshot.withPointState(reference.pathIndex(), state));
            }
        }
    }

    /** Keeps the last validated value for unloaded chunks. */
    public static synchronized void refreshLoaded(ServerLevel level, long lineId) {
        PowerSectionSnapshot previous = SNAPSHOTS.get(lineId);
        if (previous == null) return;
        PowerSectionSnapshot refreshed = PowerSectionSnapshot.createIndexed(previous.path(), SECTION_LENGTH, (index, point) -> {
            BlockPos pos = new BlockPos(point.x(), point.y(), point.z());
            return status(level, pos, previous.pointStatus(index));
        });
        SNAPSHOTS.put(lineId, refreshed);
    }

    public static boolean allPowered(long lineId) {
        PowerSectionSnapshot snapshot = SNAPSHOTS.get(lineId);
        Boolean recorded=recordedOverride(lineId,0);
        return recorded==null?snapshot != null && snapshot.allPowered():recorded;
    }

    public static boolean hasSnapshot(long lineId) {
        return SNAPSHOTS.containsKey(lineId);
    }

    public static boolean canProceed(long lineId, int pathIndex) {
        PowerSectionSnapshot snapshot = SNAPSHOTS.get(lineId);
        Boolean recorded=recordedOverride(lineId,pathIndex);
        return recorded==null?snapshot != null && snapshot.canProceedFrom(pathIndex):recorded;
    }

    public static PowerSectionStatus sectionStatus(long lineId, int sectionIndex) {
        PowerSectionSnapshot snapshot = SNAPSHOTS.get(lineId);
        long point=(long)sectionIndex*SECTION_LENGTH;
        Boolean recorded=point<0||point>Integer.MAX_VALUE?null:recordedOverride(lineId,(int)point);
        return recorded==null?snapshot == null ? PowerSectionStatus.UNKNOWN : snapshot.sectionStatus(sectionIndex):recorded?PowerSectionStatus.POWERED:PowerSectionStatus.UNPOWERED;
    }

    public static synchronized void remove(long lineId) {
        LEVELS.remove(lineId);
        PowerSectionSnapshot removed = SNAPSHOTS.remove(lineId);
        if (removed == null) return;
        for (TrackPoint point : removed.path()) {
            BlockPos pos = new BlockPos(point.x(), point.y(), point.z());
            Set<RoutePoint> references = ROUTES_BY_RAIL.get(pos);
            if (references == null) continue;
            references.removeIf(reference -> reference.lineId() == lineId);
            if (references.isEmpty()) ROUTES_BY_RAIL.remove(pos);
        }
    }

    public static synchronized void clear() {
        LEVELS.clear();
        SNAPSHOTS.clear();
        ROUTES_BY_RAIL.clear();
    }

    private static PowerSectionStatus status(ServerLevel level, BlockPos pos, PowerSectionStatus unloadedFallback) {
        if (!level.hasChunkAt(pos)) {
            Boolean verified = InfrastructureService.remotePowerAt(level, pos);
            if (verified != null) return verified ? PowerSectionStatus.POWERED : PowerSectionStatus.UNPOWERED;
            return unloadedFallback == null ? PowerSectionStatus.UNKNOWN : unloadedFallback;
        }
        return RailPowerService.isPowered(level, pos) ? PowerSectionStatus.POWERED : PowerSectionStatus.UNPOWERED;
    }

    private static Boolean recordedOverride(long lineId,int point){
        var snapshot=SNAPSHOTS.get(lineId);var reference=LEVELS.get(lineId);var level=reference==null?null:reference.get();
        if(snapshot==null||level==null||point<0||point>=snapshot.path().size())return null;
        var p=snapshot.path().get(point);
        return cn.quit5700.fabrictrain.offline.InfrastructureService.remotePowerAt(level,new BlockPos(p.x(),p.y(),p.z()));
    }

    private record RoutePoint(long lineId, int pathIndex) {}
}
