package cn.quit5700.fabrictrain.route;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwitchRailRoutingTest {
    @Test void eastboundChoicesMapToLeftStraightAndRightTracks() {
        assertEquals(RailShape.NORTH_WEST, SwitchRailRouting.shapeFor(Direction.EAST, 0));
        assertEquals(RailShape.EAST_WEST, SwitchRailRouting.shapeFor(Direction.EAST, 1));
        assertEquals(RailShape.SOUTH_WEST, SwitchRailRouting.shapeFor(Direction.EAST, 2));
    }

    @Test void northboundChoicesMapToLeftStraightAndRightTracks() {
        assertEquals(RailShape.SOUTH_WEST, SwitchRailRouting.shapeFor(Direction.NORTH, 0));
        assertEquals(RailShape.NORTH_SOUTH, SwitchRailRouting.shapeFor(Direction.NORTH, 1));
        assertEquals(RailShape.SOUTH_EAST, SwitchRailRouting.shapeFor(Direction.NORTH, 2));
    }

    @Test void westboundChoicesMapToLeftStraightAndRightTracks() {
        assertEquals(RailShape.SOUTH_EAST, SwitchRailRouting.shapeFor(Direction.WEST, 0));
        assertEquals(RailShape.EAST_WEST, SwitchRailRouting.shapeFor(Direction.WEST, 1));
        assertEquals(RailShape.NORTH_EAST, SwitchRailRouting.shapeFor(Direction.WEST, 2));
    }

    @Test void southboundChoicesMapToLeftStraightAndRightTracks() {
        assertEquals(RailShape.NORTH_EAST, SwitchRailRouting.shapeFor(Direction.SOUTH, 0));
        assertEquals(RailShape.NORTH_SOUTH, SwitchRailRouting.shapeFor(Direction.SOUTH, 1));
        assertEquals(RailShape.NORTH_WEST, SwitchRailRouting.shapeFor(Direction.SOUTH, 2));
    }

    @Test void depotSideAndChoiceProduceTheHighlightedExit() {
        assertEquals(Direction.EAST, SwitchRailRouting.exitFor(Direction.NORTH, 0));
        assertEquals(Direction.SOUTH, SwitchRailRouting.exitFor(Direction.NORTH, 1));
        assertEquals(Direction.WEST, SwitchRailRouting.exitFor(Direction.NORTH, 2));
        assertEquals(RailShape.NORTH_EAST, SwitchRailRouting.shapeForEntry(Direction.NORTH, 0));
    }

    @Test void fixedChoiceDoesNotRotateWhenTrainApproachesFromAnotherBranch() {
        assertEquals(RailShape.EAST_WEST,
                SwitchRailRouting.shapeForFixedChoice(Direction.WEST, Direction.EAST, 1).orElseThrow());
        assertEquals(RailShape.NORTH_SOUTH,
                SwitchRailRouting.shapeForFixedChoice(Direction.WEST, Direction.NORTH, 0).orElseThrow());
    }

    @Test void fixedExitCannotSendTrainBackThroughItsIncomingSide() {
        assertTrue(SwitchRailRouting.shapeForFixedChoice(Direction.WEST, Direction.WEST, 1).isEmpty());
    }
}
