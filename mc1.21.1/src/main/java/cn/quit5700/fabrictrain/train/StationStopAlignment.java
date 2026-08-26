package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.data.StopTrack;

/** Exact server position used when a locomotive reaches its line-bound station stop. */
public final class StationStopAlignment {
    private StationStopAlignment() {}

    public static Position position(StopTrack stop) {
        if (stop == null) throw new IllegalArgumentException("stop");
        return new Position(stop.x() + 0.5D, stop.y() + 0.1D, stop.z() + 0.5D);
    }

    public record Position(double x, double y, double z) {}
}
