package cn.quit5700.fabrictrain.train;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class TrainLinkStateTest {
    @Test void newAndStandaloneVehiclesAreManualByDefault(){
        TrainLink link=new TrainLink();
        assertEquals(ControlMode.MANUAL,link.controlMode());
        assertTrue(link.controlMode().playerDriveAllowed());
    }

    @Test void locomotiveStartsParkedAndAutomaticStartUnlocksIt(){TrainLink link=new TrainLink();assertTrue(link.parked());link.setAutomaticRunning(true);assertFalse(link.parked());assertTrue(link.automaticRunning());}
    @Test void detachingRestoresStandaloneIdentityAndClearsRoute(){TrainLink link=new TrainLink();UUID train=UUID.randomUUID(),locomotive=UUID.randomUUID(),car=UUID.randomUUID();link.assign(train,locomotive,4,9);link.setDestinationStationId(12);link.detach(car);assertTrue(link.standalone());assertEquals(0,link.lineId());assertEquals(0,link.destinationStationId());}
    @Test void carOnlyGroupDoesNotPretendToHaveALocomotive(){
        TrainLink first=new TrainLink(),second=new TrainLink();UUID group=UUID.randomUUID();
        first.assignWithoutLocomotive(group,0);second.assignWithoutLocomotive(group,1);
        assertFalse(first.hasLocomotive());assertFalse(second.hasLocomotive());
        assertEquals(group,first.trainId());assertEquals(group,second.trainId());
    }
    @Test void realLocomotiveCanTakeOwnershipOfWholeCarGroup(){
        TrainLink car=new TrainLink();UUID group=UUID.randomUUID(),train=UUID.randomUUID(),locomotive=UUID.randomUUID();
        car.assignWithoutLocomotive(group,1);car.assign(train,locomotive,2,7);
        assertTrue(car.hasLocomotive());assertEquals(locomotive,car.locomotiveId());assertEquals(7,car.lineId());
    }

    @Test void connectedFormationKeepsItsPositionDerivedForwardDirection(){
        TrainLink link=new TrainLink();
        assertTrue(link.formationForward().isEmpty());
        link.setFormationForward(Direction.WEST);
        assertEquals(Direction.WEST,link.formationForward().orElseThrow());
    }

    @Test void internalRegistryAddressIsInvisibleStateAndDetachClearsIt(){
        TrainLink link=new TrainLink();
        link.setRegistryAddress(2,56,3);
        assertEquals(2,link.originDepotNumber());
        assertEquals(56,link.registryTrainNumber());
        assertEquals(3,link.vehicleNumber());
        link.detach(UUID.randomUUID());
        assertEquals(0,link.originDepotNumber());
        assertEquals(0,link.registryTrainNumber());
        assertEquals(1,link.vehicleNumber());
    }
}
