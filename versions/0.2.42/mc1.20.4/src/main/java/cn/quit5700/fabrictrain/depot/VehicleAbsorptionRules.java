package cn.quit5700.fabrictrain.depot;

import java.util.Set;

public final class VehicleAbsorptionRules {
    private static final Set<String> VEHICLES = Set.of(
            "fabric_train_simulator:locomotive",
            "fabric_train_simulator:passenger_car",
            "fabric_train_simulator:cargo_car");
    private VehicleAbsorptionRules() {}
    public static boolean isVehicleId(String id) { return VEHICLES.contains(id); }
}
