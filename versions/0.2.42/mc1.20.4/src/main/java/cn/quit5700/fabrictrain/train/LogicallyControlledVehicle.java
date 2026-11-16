package cn.quit5700.fabrictrain.train;

public interface LogicallyControlledVehicle {
    boolean fabricTrain$isLogicallyControlled();
    void fabricTrain$setLogicallyControlled(boolean controlled);

    static boolean shouldRestorePosition(boolean clientSide, boolean controlled, boolean placementBraked) {
        return !clientSide && (controlled || placementBraked);
    }
}
