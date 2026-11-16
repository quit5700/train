package cn.quit5700.fabrictrain.route;

import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.properties.RailShape;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Resolves block-to-block rail connections using the same coordinates as vanilla RailState. */
public final class RailConnectionResolver {
    private RailConnectionResolver() {}

    public static List<Vec3i> connectionOffsets(RailShape shape) {
        return switch (shape) {
            case NORTH_SOUTH -> List.of(new Vec3i(0, 0, -1), new Vec3i(0, 0, 1));
            case EAST_WEST -> List.of(new Vec3i(-1, 0, 0), new Vec3i(1, 0, 0));
            case ASCENDING_EAST -> List.of(new Vec3i(-1, 0, 0), new Vec3i(1, 1, 0));
            case ASCENDING_WEST -> List.of(new Vec3i(-1, 1, 0), new Vec3i(1, 0, 0));
            case ASCENDING_NORTH -> List.of(new Vec3i(0, 1, -1), new Vec3i(0, 0, 1));
            case ASCENDING_SOUTH -> List.of(new Vec3i(0, 0, -1), new Vec3i(0, 1, 1));
            case SOUTH_EAST -> List.of(new Vec3i(0, 0, 1), new Vec3i(1, 0, 0));
            case SOUTH_WEST -> List.of(new Vec3i(0, 0, 1), new Vec3i(-1, 0, 0));
            case NORTH_WEST -> List.of(new Vec3i(0, 0, -1), new Vec3i(-1, 0, 0));
            case NORTH_EAST -> List.of(new Vec3i(0, 0, -1), new Vec3i(1, 0, 0));
        };
    }

    /**
     * Returns both shape exits in their stable vanilla order.  RailState first checks the
     * expected position, then the block above it, then the block below it.
     */
    public static List<TrackPoint> connectedCandidates(TrackPoint origin, RailShape shape,
                                                        Predicate<TrackPoint> isRail) {
        ArrayList<TrackPoint> result = new ArrayList<>(2);
        for (Vec3i offset : connectionOffsets(shape)) {
            TrackPoint expected = new TrackPoint(origin.x() + offset.getX(), origin.y() + offset.getY(),
                    origin.z() + offset.getZ());
            result.add(resolveExpected(expected, isRail));
        }
        return List.copyOf(result);
    }

    public static List<TrackPoint> neighbours(TrackPoint origin, RailShape shape,
                                               Predicate<TrackPoint> isRail) {
        return connectedCandidates(origin, shape, isRail).stream().filter(isRail).distinct().toList();
    }

    private static TrackPoint resolveExpected(TrackPoint expected, Predicate<TrackPoint> isRail) {
        if (isRail.test(expected)) return expected;
        TrackPoint above = new TrackPoint(expected.x(), expected.y() + 1, expected.z());
        if (isRail.test(above)) return above;
        TrackPoint below = new TrackPoint(expected.x(), expected.y() - 1, expected.z());
        return isRail.test(below) ? below : expected;
    }
}
