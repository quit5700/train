package cn.quit5700.fabrictrain.train;

public enum ControlMode {
    MANUAL,
    ROUTE_AUTOMATIC;

    public boolean playerDriveAllowed() {
        return this == MANUAL;
    }
}
