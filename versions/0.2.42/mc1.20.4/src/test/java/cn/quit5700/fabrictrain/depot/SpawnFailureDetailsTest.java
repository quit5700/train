package cn.quit5700.fabrictrain.depot;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class SpawnFailureDetailsTest {
 @Test void locomotiveDetailsHaveThreeCoordinates(){var o=DepotDispatchService.DispatchOutcome.failureAt(DepotDispatchResult.SPAWN_ENTITY_BLOCKED,0,new BlockPos(64,-60,52));assertEquals("message.fabric_train_simulator.depot.spawn_entity_blocked.locomotive",o.messageKey());assertArrayEquals(new String[]{"64","-60","52"},o.messageArguments());assertTrue(o.spawned().isEmpty());assertFalse(o.succeeded());}
 @Test void fifthCarDetailsPreserveIndexAndMissingPosition(){var o=DepotDispatchService.DispatchOutcome.failureAt(DepotDispatchResult.SPAWN_MISSING_TRACK,5,new BlockPos(64,-60,46));assertTrue(o.messageKey().endsWith(".car"));assertArrayEquals(new String[]{"5","64","-60","46"},o.messageArguments());}
 @Test void legacyFailureDoesNotInventCoordinates(){var o=DepotDispatchService.DispatchOutcome.failure(DepotDispatchResult.UNPOWERED);assertEquals(DepotDispatchResult.UNPOWERED.translationKey(),o.messageKey());assertEquals(0,o.messageArguments().length);}
 @Test void oldSavedResultOrdinalsRemainStable(){assertEquals(6,DepotDispatchResult.TRACK_SPACE_INVALID.ordinal());assertEquals(7,DepotDispatchResult.SPAWN_FAILED.ordinal());assertTrue(DepotDispatchResult.SPAWN_MISSING_TRACK.ordinal()>7);}
}
