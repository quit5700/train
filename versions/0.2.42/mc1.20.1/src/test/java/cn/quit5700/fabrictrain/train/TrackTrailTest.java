package cn.quit5700.fabrictrain.train;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrackTrailTest {
    @Test
    void prefilledFormationImmediatelyProvidesFollowerTargets() {
        TrackTrail trail = new TrackTrail(20.0D);
        trail.record(0, 0, -FormationSpacing.BLOCKS * 2);
        trail.record(0, 0, -FormationSpacing.BLOCKS);
        trail.record(0, 0, 0);

        TrackTrail.Point firstCar = trail.pointBehind(FormationSpacing.BLOCKS);
        TrackTrail.Point secondCar = trail.pointBehind(FormationSpacing.BLOCKS * 2);

        assertEquals(-FormationSpacing.BLOCKS, firstCar.z(), 0.0001D);
        assertEquals(-FormationSpacing.BLOCKS * 2, secondCar.z(), 0.0001D);
    }
    @Test
    void samplesFollowerPositionAlongLocomotivePath() {
        TrackTrail trail = new TrackTrail(20);
        for (int x = 0; x <= 10; x++) trail.record(x, 64, 0);
        TrackTrail.Point target = trail.pointBehind(2.5D);
        assertNotNull(target);
        assertEquals(7.5D, target.x(), 0.0001D);
        assertEquals(64.0D, target.y(), 0.0001D);
    }

    @Test
    void returnsNullUntilEnoughTrackWasRecorded() {
        TrackTrail trail = new TrackTrail(20);
        trail.record(0, 64, 0);
        assertNull(trail.pointBehind(2));
    }
}
