package cn.quit5700.fabrictrain.depot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DepotDirectionRulesTest {
    @Test void nodeSideIsFrontAndEmptySideIsRear() {
        assertEquals(0, DepotDirectionRules.frontIndex(true, false));
        assertEquals(1, DepotDirectionRules.frontIndex(false, true));
    }

    @Test void ambiguousSidesAreRejected() {
        assertEquals(-1, DepotDirectionRules.frontIndex(false, false));
        assertEquals(-1, DepotDirectionRules.frontIndex(true, true));
    }

    @Test void selectedLineChoosesShorterReachableSide() {
        assertEquals(0, DepotDirectionRules.frontIndex(8, 19));
        assertEquals(1, DepotDirectionRules.frontIndex(40, 7));
    }

    @Test void selectedLineRejectsEqualOrMissingPaths() {
        assertEquals(-1, DepotDirectionRules.frontIndex(8, 8));
        assertEquals(-1, DepotDirectionRules.frontIndex(-1, -1));
        assertEquals(0, DepotDirectionRules.frontIndex(8, -1));
    }
}
