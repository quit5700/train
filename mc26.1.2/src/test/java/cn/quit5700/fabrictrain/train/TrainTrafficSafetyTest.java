package cn.quit5700.fabrictrain.train;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainTrafficSafetyTest {
    @Test void blocksOnlyAnotherTrainAheadOnTheSameTrackCorridor() {
        double required = TrainTrafficSafety.stoppingDistance(1.0D);
        assertTrue(TrainTrafficSafety.blocks(required - 0.1D, 0.25D, 0.0D, required));
        assertFalse(TrainTrafficSafety.blocks(-1.0D, 0.0D, 0.0D, required));
        assertFalse(TrainTrafficSafety.blocks(required - 0.1D, 9.0D, 0.0D, required));
        assertFalse(TrainTrafficSafety.blocks(required - 0.1D, 0.0D, 4.0D, required));
    }
}
