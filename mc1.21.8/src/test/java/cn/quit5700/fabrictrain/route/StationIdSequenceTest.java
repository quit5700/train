package cn.quit5700.fabrictrain.route;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StationIdSequenceTest {
    @Test
    void stationIdsArePermanentAndNeverReused() {
        StationIdSequence sequence = new StationIdSequence(40L);
        assertEquals(41L, sequence.issue());
        assertEquals(42L, sequence.issue());
        assertEquals(43L, sequence.nextId());
    }
}
