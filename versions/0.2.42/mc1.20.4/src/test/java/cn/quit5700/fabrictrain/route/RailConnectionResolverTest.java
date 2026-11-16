package cn.quit5700.fabrictrain.route;

import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RailConnectionResolverTest {
    @Test
    void ascendingConnectionsMatchVanillaRailStateCoordinates() {
        assertEquals(List.of(new Vec3i(-1, 0, 0), new Vec3i(1, 1, 0)),
                RailConnectionResolver.connectionOffsets(RailShape.ASCENDING_EAST));
        assertEquals(List.of(new Vec3i(-1, 1, 0), new Vec3i(1, 0, 0)),
                RailConnectionResolver.connectionOffsets(RailShape.ASCENDING_WEST));
        assertEquals(List.of(new Vec3i(0, 1, -1), new Vec3i(0, 0, 1)),
                RailConnectionResolver.connectionOffsets(RailShape.ASCENDING_NORTH));
        assertEquals(List.of(new Vec3i(0, 0, -1), new Vec3i(0, 1, 1)),
                RailConnectionResolver.connectionOffsets(RailShape.ASCENDING_SOUTH));
    }

    @Test
    void tenBlockSlopesAreConnectedBothWaysInAllFourDirections() {
        assertContinuousSlope(RailShape.ASCENDING_EAST, 1, 0, RailShape.EAST_WEST);
        assertContinuousSlope(RailShape.ASCENDING_WEST, -1, 0, RailShape.EAST_WEST);
        assertContinuousSlope(RailShape.ASCENDING_NORTH, 0, -1, RailShape.NORTH_SOUTH);
        assertContinuousSlope(RailShape.ASCENDING_SOUTH, 0, 1, RailShape.NORTH_SOUTH);
    }

    @Test
    void raisedHumpConnectsUphillTopAndDownhillInBothDirections() {
        Map<TrackPoint, RailShape> rails = new HashMap<>();
        List<TrackPoint> expected = List.of(
                new TrackPoint(-2, 64, 0), new TrackPoint(-1, 64, 0),
                new TrackPoint(0, 65, 0), new TrackPoint(1, 64, 0),
                new TrackPoint(2, 64, 0));
        rails.put(expected.get(0), RailShape.EAST_WEST);
        rails.put(expected.get(1), RailShape.ASCENDING_EAST);
        rails.put(expected.get(2), RailShape.EAST_WEST);
        rails.put(expected.get(3), RailShape.ASCENDING_WEST);
        rails.put(expected.get(4), RailShape.EAST_WEST);

        assertPathBothWays(expected, rails);
    }

    private static void assertContinuousSlope(RailShape slopeShape, int dx, int dz, RailShape flatShape) {
        Map<TrackPoint, RailShape> rails = new HashMap<>();
        TrackPoint lowerFlat = new TrackPoint(-dx, 64, -dz);
        TrackPoint upperFlat = new TrackPoint(dx * 10, 74, dz * 10);
        rails.put(lowerFlat, flatShape);
        ArrayList<TrackPoint> expected = new ArrayList<>();
        expected.add(lowerFlat);
        for (int step = 0; step < 10; step++) {
            TrackPoint slope = new TrackPoint(dx * step, 64 + step, dz * step);
            rails.put(slope, slopeShape);
            expected.add(slope);
        }
        rails.put(upperFlat, flatShape);
        expected.add(upperFlat);
        assertPathBothWays(expected, rails);
    }

    private static void assertPathBothWays(List<TrackPoint> expected, Map<TrackPoint, RailShape> rails) {
        java.util.function.Function<TrackPoint, java.util.Collection<TrackPoint>> neighbours = point -> {
            RailShape shape = rails.get(point);
            return shape == null ? List.of()
                    : RailConnectionResolver.neighbours(point, shape, rails::containsKey);
        };
        PathScanResult forward = TrackPathScanner.scan(expected.get(0), expected.get(expected.size() - 1), 100, neighbours);
        PathScanResult reverse = TrackPathScanner.scan(expected.get(expected.size() - 1), expected.get(0), 100, neighbours);

        assertEquals(PathScanResult.Status.FOUND, forward.status());
        assertEquals(expected, forward.path());
        assertEquals(PathScanResult.Status.FOUND, reverse.status());
        ArrayList<TrackPoint> reversed = new ArrayList<>(expected);
        java.util.Collections.reverse(reversed);
        assertEquals(reversed, reverse.path());
    }

    @Test
    void flatAndCurvedRailsKeepTheirOriginalHorizontalConnections() {
        assertEquals(List.of(new Vec3i(0, 0, -1), new Vec3i(0, 0, 1)),
                RailConnectionResolver.connectionOffsets(RailShape.NORTH_SOUTH));
        assertEquals(List.of(new Vec3i(-1, 0, 0), new Vec3i(1, 0, 0)),
                RailConnectionResolver.connectionOffsets(RailShape.EAST_WEST));
        assertEquals(List.of(new Vec3i(0, 0, 1), new Vec3i(1, 0, 0)),
                RailConnectionResolver.connectionOffsets(RailShape.SOUTH_EAST));
        assertEquals(List.of(new Vec3i(0, 0, -1), new Vec3i(-1, 0, 0)),
                RailConnectionResolver.connectionOffsets(RailShape.NORTH_WEST));
    }
}

