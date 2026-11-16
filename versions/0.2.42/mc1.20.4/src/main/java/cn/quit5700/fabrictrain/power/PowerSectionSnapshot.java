package cn.quit5700.fabrictrain.power;

import cn.quit5700.fabrictrain.route.TrackPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.BiFunction;

/** Immutable route power snapshot split by a fixed number of track blocks. */
public final class PowerSectionSnapshot {
    private final List<TrackPoint> path;
    private final int sectionLength;
    private final List<PowerSectionStatus> pointStates;

    private PowerSectionSnapshot(List<TrackPoint> path, int sectionLength, List<PowerSectionStatus> pointStates) {
        this.path = List.copyOf(path);
        this.sectionLength = Math.max(1, sectionLength);
        this.pointStates = List.copyOf(pointStates);
    }

    public static PowerSectionSnapshot create(List<TrackPoint> path, int sectionLength,
                                              Function<TrackPoint, PowerSectionStatus> resolver) {
        return createIndexed(path, sectionLength, (index, point) -> resolver.apply(point));
    }

    public static PowerSectionSnapshot createIndexed(List<TrackPoint> path, int sectionLength,
                                                      BiFunction<Integer, TrackPoint, PowerSectionStatus> resolver) {
        ArrayList<PowerSectionStatus> states = new ArrayList<>();
        for (int index = 0; index < path.size(); index++) {
            PowerSectionStatus state = resolver.apply(index, path.get(index));
            states.add(state == null ? PowerSectionStatus.UNKNOWN : state);
        }
        return new PowerSectionSnapshot(path, sectionLength, states);
    }

    public int sectionCount() {
        return path.isEmpty() ? 0 : (path.size() + sectionLength - 1) / sectionLength;
    }

    public PowerSectionStatus sectionStatus(int sectionIndex) {
        if (sectionIndex < 0 || sectionIndex >= sectionCount()) return PowerSectionStatus.UNKNOWN;
        int start = sectionIndex * sectionLength;
        int end = Math.min(pointStates.size(), start + sectionLength);
        boolean unknown = false;
        for (int index = start; index < end; index++) {
            PowerSectionStatus state = pointStates.get(index);
            if (state == PowerSectionStatus.UNPOWERED) return PowerSectionStatus.UNPOWERED;
            if (state == PowerSectionStatus.UNKNOWN) unknown = true;
        }
        return unknown ? PowerSectionStatus.UNKNOWN : PowerSectionStatus.POWERED;
    }

    public boolean allPowered() {
        for (int section = 0; section < sectionCount(); section++)
            if (sectionStatus(section) != PowerSectionStatus.POWERED) return false;
        return !path.isEmpty();
    }

    public boolean canProceedFrom(int pathIndex) {
        if (path.isEmpty()) return false;
        int current = cn.quit5700.fabrictrain.compat.MathCompat.clamp(pathIndex, 0, path.size() - 1) / sectionLength;
        if (sectionStatus(current) != PowerSectionStatus.POWERED) return false;
        return current + 1 >= sectionCount() || sectionStatus(current + 1) == PowerSectionStatus.POWERED;
    }

    public List<TrackPoint> path() { return path; }
    public int sectionLength() { return sectionLength; }
    public PowerSectionStatus pointStatus(int pathIndex) {
        return pathIndex < 0 || pathIndex >= pointStates.size()
                ? PowerSectionStatus.UNKNOWN : pointStates.get(pathIndex);
    }

    public PowerSectionSnapshot withPointState(int pathIndex, PowerSectionStatus state) {
        if (pathIndex < 0 || pathIndex >= pointStates.size()) return this;
        ArrayList<PowerSectionStatus> updated = new ArrayList<>(pointStates);
        updated.set(pathIndex, state == null ? PowerSectionStatus.UNKNOWN : state);
        return new PowerSectionSnapshot(path, sectionLength, updated);
    }
}

