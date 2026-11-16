package cn.quit5700.fabrictrain.train;

/** Per-tick following decision; it is never persisted and therefore cannot become a ghost lock. */
public record FollowingTrainDecision(boolean hardStop, double speedLimit) {
    private static final double MOVING_EPSILON = 0.0001D;

    public static FollowingTrainDecision evaluate(boolean relevantTrainAhead, boolean sameDirection,
                                                  double frontTrainSpeed) {
        if (!relevantTrainAhead) return clear();
        double safeFrontSpeed = Math.max(0.0D, Math.abs(frontTrainSpeed));
        if (!sameDirection || safeFrontSpeed <= MOVING_EPSILON)
            return new FollowingTrainDecision(true, 0.0D);
        return new FollowingTrainDecision(false, safeFrontSpeed);
    }

    public static FollowingTrainDecision clear() {
        return new FollowingTrainDecision(false, Double.POSITIVE_INFINITY);
    }

    public FollowingTrainDecision restrictWith(FollowingTrainDecision other) {
        if (other == null) return this;
        return new FollowingTrainDecision(hardStop || other.hardStop,
                Math.min(speedLimit, other.speedLimit));
    }
}
