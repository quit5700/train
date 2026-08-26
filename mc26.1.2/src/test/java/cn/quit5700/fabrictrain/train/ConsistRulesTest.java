package cn.quit5700.fabrictrain.train;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConsistRulesTest {
    @Test
    void cargoCarForcesCargoClass() {
        assertEquals(TrainClass.CARGO, ConsistRules.normalizeClass(
                List.of(CarType.PASSENGER, CarType.CARGO), TrainClass.EXPRESS));
    }

    @Test
    void passengerOnlyTrainCanBeExpress() {
        assertEquals(TrainClass.EXPRESS, ConsistRules.normalizeClass(
                List.of(CarType.PASSENGER), TrainClass.EXPRESS));
    }

    @Test
    void passengerOnlyTrainCanChooseCargoSpeedClass() {
        assertEquals(TrainClass.CARGO, ConsistRules.normalizeClass(
                List.of(CarType.PASSENGER), TrainClass.CARGO));
    }

    @Test
    void absoluteLimitIsOneHundredCars() {
        assertTrue(ConsistRules.validCarCount(100, 1000));
        assertFalse(ConsistRules.validCarCount(101, 1000));
    }
}
