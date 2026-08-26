package cn.quit5700.fabrictrain.depot;

public enum DepotDispatchResult {
    SUCCESS("message.fabric_train_simulator.depot.dispatch_success"),
    EMPTY_TEMPLATE("message.fabric_train_simulator.depot.empty_template"),
    NO_RAIL("message.fabric_train_simulator.depot.no_rail"),
    UNPOWERED("message.fabric_train_simulator.depot.unpowered"),
    MISSING_VEHICLE_ITEMS("message.fabric_train_simulator.depot.missing_vehicle_items"),
    DIRECTION_UNRESOLVED("message.fabric_train_simulator.depot.direction_unresolved"),
    TRACK_SPACE_INVALID("message.fabric_train_simulator.depot.track_space_invalid"),
    SPAWN_FAILED("message.fabric_train_simulator.depot.spawn_failed");

    private final String translationKey;

    DepotDispatchResult(String translationKey) {
        this.translationKey = translationKey;
    }

    public boolean succeeded() {
        return this == SUCCESS;
    }

    public String translationKey() {
        return translationKey;
    }
}
