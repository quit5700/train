package cn.quit5700.fabrictrain.route;

import java.util.List;

public record PathScanResult(Status status, List<TrackPoint> path) {
    public enum Status {
        FOUND,
        NOT_FOUND,
        TOO_LONG
    }

    public PathScanResult {
        path = List.copyOf(path);
    }
}
