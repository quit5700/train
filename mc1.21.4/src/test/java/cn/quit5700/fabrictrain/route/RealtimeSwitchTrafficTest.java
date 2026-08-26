package cn.quit5700.fabrictrain.route;

import cn.quit5700.fabrictrain.train.TrainTrafficSafety;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealtimeSwitchTrafficTest {
    @Test void anActuallyOccupyingTrainBeatsAnApproachingTrain() {
        UUID occupying = new UUID(0, 2);
        UUID approaching = new UUID(0, 1);
        assertEquals(occupying, RealtimeSwitchTraffic.chooseWinner(List.of(
                new RealtimeSwitchTraffic.Candidate(approaching, 1.0D, false),
                new RealtimeSwitchTraffic.Candidate(occupying, 9.0D, true))).orElseThrow());
    }

    @Test void nearestApproachWinsAndAbsentTrainsCannotLeaveGhostOwnership() {
        UUID near = new UUID(0, 2);
        UUID far = new UUID(0, 1);
        assertEquals(near, RealtimeSwitchTraffic.chooseWinner(List.of(
                new RealtimeSwitchTraffic.Candidate(far, 25.0D, false),
                new RealtimeSwitchTraffic.Candidate(near, 4.0D, false))).orElseThrow());
        assertTrue(RealtimeSwitchTraffic.chooseWinner(List.of()).isEmpty());
    }

    @Test void reservationDistanceCoversEmergencyStoppingDistance() {
        assertTrue(RealtimeSwitchTraffic.reservationDistance(2.5D)
                >= TrainTrafficSafety.stoppingDistance(2.5D));
    }
}
