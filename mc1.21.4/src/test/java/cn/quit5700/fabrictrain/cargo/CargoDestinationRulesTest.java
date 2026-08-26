package cn.quit5700.fabrictrain.cargo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CargoDestinationRulesTest {
    @Test void independentCargoDestinationWinsWhenConnected(){
        assertEquals(9L,CargoDestinationRules.afterConnecting(9L,4L));
    }
    @Test void unsetCargoInheritsLocomotiveBatchDestination(){
        assertEquals(4L,CargoDestinationRules.afterConnecting(0L,4L));
    }
    @Test void destinationUnloadOnlyTriggersInsideStationRange(){
        org.junit.jupiter.api.Assertions.assertTrue(CargoDestinationRules.atStation(10.5,64.5,10.5,10.5,64.5,10.5));
        org.junit.jupiter.api.Assertions.assertFalse(CargoDestinationRules.atStation(13,64.5,10.5,10.5,64.5,10.5));
    }
}
