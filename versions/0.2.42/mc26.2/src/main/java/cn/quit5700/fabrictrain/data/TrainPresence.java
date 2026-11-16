package cn.quit5700.fabrictrain.data;

public enum TrainPresence {
    LOADED(true),
    UNLOADED(true),
    OFFLINE(true),
    DESTROYED(false),
    LEGACY_UNCONFIRMED(false);

    private final boolean visible;

    TrainPresence(boolean visible) { this.visible = visible; }
    public boolean visible() { return visible; }
}
