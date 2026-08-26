package cn.quit5700.fabrictrain.train;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpeedRulesTest {
    @Test
    void lowerOfLineAndClassLimitWins() {
        assertEquals(20.0D, SpeedRules.actualSpeed(20.0D, 30.0D));
        assertEquals(30.0D, SpeedRules.actualSpeed(40.0D, 30.0D));
    }

    @Test void depotConfiguredLimitAlsoParticipates() {
        assertEquals(25D,SpeedRules.actualSpeed(80D,40D,25D));
        assertEquals(40D,SpeedRules.actualSpeed(80D,40D,0D));
    }

    @Test void configuredLimitRejectsInvalidInputAndCapsExtremeValues() {
        assertEquals(28.8D,SpeedRules.sanitizeConfiguredLimit(Double.NaN,28.8D));
        assertEquals(28.8D,SpeedRules.sanitizeConfiguredLimit(0D,28.8D));
        assertEquals(SpeedRules.MAX_CONFIGURED_LIMIT_KMH,SpeedRules.sanitizeConfiguredLimit(5000D,28.8D));
    }

    @Test
    void expressIsOneAndAHalfTimesPassenger() {
        assertEquals(45.0D, SpeedRules.expressLimit(30.0D));
    }

    @Test
    void invalidSpeedIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> SpeedRules.actualSpeed(-1.0D, 10.0D));
        assertThrows(IllegalArgumentException.class, () -> SpeedRules.actualSpeed(Double.NaN, 10.0D));
    }

    @Test
    void convertsKmhAndClassLimits() {
        assertEquals(0.4D, SpeedRules.blocksPerTick(SpeedRules.FREIGHT_LIMIT_KMH), 0.000001D);
        assertEquals(SpeedRules.PASSENGER_LIMIT_KMH * 1.5D, SpeedRules.classLimit(TrainClass.EXPRESS));
    }
}
