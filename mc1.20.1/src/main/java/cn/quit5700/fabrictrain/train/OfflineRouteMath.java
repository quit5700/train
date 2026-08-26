package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.route.TrackPoint;

import java.util.List;

public final class OfflineRouteMath {
    private OfflineRouteMath() {}

    public static Position advance(List<TrackPoint> path, boolean loop, int loopStart,
                                   int pathIndex, double progress, double distance) {
        if (path.size() < 2) return new Position(0, 0D, true);
        int index = cn.quit5700.fabrictrain.compat.MathCompat.clamp(pathIndex, 0, path.size() - 1);
        double remaining = Math.max(0D, distance);
        double along = Math.max(0D, progress);
        int budget = Math.max(64, path.size() * 2);
        while (remaining > 0.000001D && budget-- > 0) {
            int next = nextIndex(path, loop, loopStart, index);
            if (next < 0) return new Position(index, 0D, true);
            double length = distance(path.get(index), path.get(next));
            double available = Math.max(0D, length - along);
            if (remaining <= available) return new Position(index, along + remaining, false);
            remaining -= available;
            index = next;
            along = 0D;
        }
        return new Position(index, along, false);
    }

    public static int nextIndex(List<TrackPoint> path, boolean loop, int loopStart, int index) {
        if (index + 1 < path.size()) return index + 1;
        return loop ? cn.quit5700.fabrictrain.compat.MathCompat.clamp(loopStart, 0, path.size() - 2) : -1;
    }

    public static double distance(TrackPoint a, TrackPoint b) {
        double dx = b.x() - a.x(), dy = b.y() - a.y(), dz = b.z() - a.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public record Position(int pathIndex, double progress, boolean ended) {}
}

