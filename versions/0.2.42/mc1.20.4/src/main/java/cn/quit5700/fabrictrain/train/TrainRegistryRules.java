package cn.quit5700.fabrictrain.train;

public final class TrainRegistryRules {
    private TrainRegistryRules() {}

    public static boolean canMerge(ControlMode first, ControlMode second) {
        return first == second;
    }

    public static int inheritedSide(boolean frontHasLocomotive, boolean rearHasLocomotive) {
        return rearHasLocomotive && !frontHasLocomotive ? 1 : 0;
    }
}
