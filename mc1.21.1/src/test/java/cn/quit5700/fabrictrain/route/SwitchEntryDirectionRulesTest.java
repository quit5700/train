package cn.quit5700.fabrictrain.route;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SwitchEntryDirectionRulesTest {
    @Test
    void persistedEntryIsUsedEvenWhenAnotherDepotIsNearby() {
        assertEquals(Direction.WEST, SwitchEntryDirectionRules.resolve(
                Direction.WEST.ordinal(), Direction.NORTH));
    }

    @Test
    void placementDirectionIsUsedForOldWorldsWithoutSavedEntry() {
        assertEquals(Direction.SOUTH, SwitchEntryDirectionRules.resolve(-1, Direction.SOUTH));
    }

    @Test
    void verticalOrInvalidSavedValuesCannotBecomeSwitchEntries() {
        assertEquals(Direction.EAST, SwitchEntryDirectionRules.resolve(Direction.UP.ordinal(), Direction.EAST));
        assertEquals(Direction.NORTH, SwitchEntryDirectionRules.resolve(99, Direction.NORTH));
    }
}
