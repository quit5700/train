package cn.quit5700.fabrictrain.depot;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VehicleAbsorptionRulesTest {
    @Test void onlyModTrainVehiclesAreAbsorbed() {
        assertTrue(VehicleAbsorptionRules.isVehicleId("fabric_train_simulator:locomotive"));
        assertTrue(VehicleAbsorptionRules.isVehicleId("fabric_train_simulator:passenger_car"));
        assertTrue(VehicleAbsorptionRules.isVehicleId("fabric_train_simulator:cargo_car"));
        assertFalse(VehicleAbsorptionRules.isVehicleId("minecraft:minecart"));
        assertFalse(VehicleAbsorptionRules.isVehicleId("minecraft:iron_ingot"));
    }
}
