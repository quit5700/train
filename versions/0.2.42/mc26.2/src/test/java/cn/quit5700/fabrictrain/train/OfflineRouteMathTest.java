package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.route.TrackPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OfflineRouteMathTest {
    private static final List<TrackPoint> SLOPE = List.of(
            new TrackPoint(0, 0, 0), new TrackPoint(1, 1, 0), new TrackPoint(2, 1, 0));

    @Test void advancesByRealThreeDimensionalDistanceOnSlope() {
        OfflineRouteMath.Position halfway = OfflineRouteMath.advance(SLOPE, false, 0,
                0, 0D, Math.sqrt(2D) / 2D);
        assertEquals(0, halfway.pathIndex());
        assertEquals(Math.sqrt(2D) / 2D, halfway.progress(), 0.00001D);
        OfflineRouteMath.Position top = OfflineRouteMath.advance(SLOPE, false, 0,
                halfway.pathIndex(), halfway.progress(), Math.sqrt(2D) / 2D);
        assertEquals(0, top.pathIndex());
        assertEquals(Math.sqrt(2D), top.progress(), 0.00001D);
    }

    @Test void loopsFromLastPointToConfiguredLoopStart() {
        OfflineRouteMath.Position result = OfflineRouteMath.advance(SLOPE, true, 1,
                2, 0D, 0.25D);
        assertEquals(2, result.pathIndex());
        assertEquals(0.25D, result.progress(), 0.00001D);
        assertFalse(result.ended());
    }

    @Test void stopsAtEndOfNonLoopRoute() {
        assertTrue(OfflineRouteMath.advance(SLOPE, false, 0, 1, 0D, 3D).ended());
    }
}
