package cn.quit5700.fabrictrain.cargo;

public final class ManualUnloadRules {
    private ManualUnloadRules() {}
    public static int movable(boolean matchingItem, int sourceCount, int destinationCapacity) {
        if (!matchingItem) return 0;
        return Math.min(Math.max(0, sourceCount), Math.max(0, destinationCapacity));
    }
}
