package cn.quit5700.fabrictrain.depot;

import cn.quit5700.fabrictrain.data.LineStatus;

public final class DepotDispatchPrecheck {
    private DepotDispatchPrecheck() {}

    public static Result check(LineStatus status, long active, int maximum) {
        if (status == null) return Result.NO_LINE;
        if (active >= Math.max(1, maximum)) return Result.MAXIMUM_REACHED;
        return switch (status) {
            case RUNNING -> Result.READY;
            case PAUSED -> Result.PAUSED;
            case CLOSED -> Result.CLOSED;
            case MAINTENANCE -> Result.MAINTENANCE;
            case NEEDS_VALIDATION -> Result.NEEDS_VALIDATION;
        };
    }

    public enum Result {
        READY,
        NO_LINE,
        MAXIMUM_REACHED,
        PAUSED,
        CLOSED,
        MAINTENANCE,
        NEEDS_VALIDATION
    }
}
