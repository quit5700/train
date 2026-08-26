package cn.quit5700.fabrictrain.cargo;

public final class CargoUnloadRules {
    private CargoUnloadRules() {}

    public static long effectiveDestination(long carriageDestination, long locomotiveDestination) {
        return carriageDestination > 0L ? carriageDestination : Math.max(0L, locomotiveDestination);
    }

    public static boolean shouldUnloadAt(long currentStation, long carriageDestination, long locomotiveDestination) {
        long effective = effectiveDestination(carriageDestination, locomotiveDestination);
        return effective > 0L && currentStation == effective;
    }
}
