package cn.quit5700.fabrictrain.cargo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CargoTransferTest {
    @Test
    void reservesOnlyAvailableCapacity() {
        CargoTransfer transfer = CargoTransfer.reserve(7L, 64, 20);
        assertEquals(20, transfer.reservedItems());
        assertEquals(64, transfer.requestedItems());
    }

    @Test
    void unloadAcceptsAnEmptyDestinationSlotWithoutAMatchingStack() {
        assertEquals(32, CargoSlotRules.movable(true, false, true, 32, 64));
        assertEquals(0, CargoSlotRules.movable(true, false, false, 32, 64));
        assertEquals(5, CargoSlotRules.movable(false, true, true, 32, 5));
        assertEquals(0, CargoSlotRules.movable(false, false, true, 32, 64));
    }
}
