package cn.quit5700.fabrictrain.train;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RailMotionCursorRouteShapeTest {
    @Test
    void cachedRouteChoosesItsOwnCurveInsteadOfManualSwitchChoice() {
        BlockPos west = new BlockPos(-1, 0, 0);
        BlockPos crossing = BlockPos.ZERO;
        BlockPos north = new BlockPos(0, 0, -1);

        assertEquals(RailShape.NORTH_WEST, RailMotionCursor.routeShapeAt(
                List.of(west, crossing, north), false, 0, 0, null, crossing).orElseThrow());
    }

    @Test
    void repeatedSwitchUsesTheNextOccurrenceInTheCachedRoute() {
        BlockPos crossing = BlockPos.ZERO;
        List<BlockPos> route = List.of(
                new BlockPos(-1, 0, 0), crossing, new BlockPos(0, 0, -1),
                new BlockPos(1, 0, -1), new BlockPos(1, 0, 0), crossing, new BlockPos(0, 0, 1));

        assertEquals(RailShape.SOUTH_EAST, RailMotionCursor.routeShapeAt(
                route, false, 0, 3, route.get(2), crossing).orElseThrow());
    }
}
