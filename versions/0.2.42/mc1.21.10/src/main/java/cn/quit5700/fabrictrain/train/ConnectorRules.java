package cn.quit5700.fabrictrain.train;

public final class ConnectorRules {
    public static final double MAX_DISTANCE_SQUARED = 16.0D * 16.0D;
    public static final int MAX_CARS = 100;

    private ConnectorRules() {}

    public static boolean withinRange(double distanceSquared) {
        return distanceSquared <= MAX_DISTANCE_SQUARED;
    }

    public static boolean canMerge(int firstSize, int secondSize) {
        return firstSize > 0 && secondSize > 0 && firstSize + secondSize <= MAX_CARS + 1;
    }
}
