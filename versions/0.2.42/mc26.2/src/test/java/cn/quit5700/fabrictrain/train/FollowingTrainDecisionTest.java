package cn.quit5700.fabrictrain.train;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FollowingTrainDecisionTest {
    @Test void stationaryOrOpposingTrainRequiresAFullStop() {
        assertTrue(FollowingTrainDecision.evaluate(true, true, 0.0D).hardStop());
        assertTrue(FollowingTrainDecision.evaluate(true, false, 0.4D).hardStop());
    }

    @Test void movingTrainInSameDirectionLetsTheQueueMoveImmediatelyAtItsSpeed() {
        FollowingTrainDecision decision = FollowingTrainDecision.evaluate(true, true, 0.35D);
        assertFalse(decision.hardStop());
        assertEquals(0.35D, decision.speedLimit(), 0.0001D);
    }

    @Test void noRelevantTrainLeavesSpeedUnlimited() {
        FollowingTrainDecision decision = FollowingTrainDecision.evaluate(false, true, 0.0D);
        assertFalse(decision.hardStop());
        assertEquals(Double.POSITIVE_INFINITY, decision.speedLimit());
    }
}
