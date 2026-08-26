package cn.quit5700.fabrictrain.depot;

public final class AutomaticDispatchSchedule {
    private AutomaticDispatchSchedule() {}

    public static boolean shouldAttempt(boolean enabled, boolean selectedLineMatches,
                                        long activeTrains, int maximumTrains,
                                        long currentTick, long lastDispatchTick,
                                        int intervalSeconds) {
        if (!enabled || !selectedLineMatches || activeTrains >= Math.max(1, maximumTrains)) return false;
        long requiredTicks = (long) Math.max(1, intervalSeconds) * 20L;
        return currentTick - lastDispatchTick >= requiredTicks;
    }
}
