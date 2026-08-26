package cn.quit5700.fabrictrain.cargo;

public final class CargoSlotRules {
    private CargoSlotRules() {}

    public static int movable(boolean targetEmpty, boolean sameItem, boolean canPlace,
                              int sourceCount, int destinationCapacity) {
        if (!canPlace || (!targetEmpty && !sameItem)) return 0;
        return Math.min(Math.max(0, sourceCount), Math.max(0, destinationCapacity));
    }
}
