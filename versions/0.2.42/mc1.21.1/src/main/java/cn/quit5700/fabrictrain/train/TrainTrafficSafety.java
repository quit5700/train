package cn.quit5700.fabrictrain.train;

public final class TrainTrafficSafety {
    public static final double EMERGENCY_DECELERATION = 0.08D;
    private static final double MARGIN = 8.0D;
    private static final double TRACK_CORRIDOR_SQUARED = 2.25D;
    private static final double VERTICAL_TOLERANCE = 2.0D;

    private TrainTrafficSafety() {}

    public static double stoppingDistance(double speed) {
        double safeSpeed = Math.max(0.0D, Math.abs(speed));
        return MARGIN + safeSpeed * safeSpeed / (2.0D * EMERGENCY_DECELERATION);
    }

    public static boolean blocks(double forwardDistance, double lateralDistanceSquared,
                                 double verticalDistance, double requiredDistance) {
        return forwardDistance > 0.0D && forwardDistance <= requiredDistance
                && lateralDistanceSquared <= TRACK_CORRIDOR_SQUARED
                && Math.abs(verticalDistance) <= VERTICAL_TOLERANCE;
    }
}
