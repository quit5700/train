package cn.quit5700.fabrictrain.route;

import cn.quit5700.fabrictrain.power.PowerSectionCache;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RuntimePathCache {
    private static final Map<Long, List<TrackPoint>> PATHS = new ConcurrentHashMap<>();
    private RuntimePathCache() {}

    public static void put(long lineId, List<TrackPoint> path) { PATHS.put(lineId, List.copyOf(path)); }
    public static List<TrackPoint> get(long lineId) { return PATHS.getOrDefault(lineId, List.of()); }
    public static void remove(long lineId) { PATHS.remove(lineId); PowerSectionCache.remove(lineId); }
    public static void clear() { PATHS.clear(); PowerSectionCache.clear(); }
}
