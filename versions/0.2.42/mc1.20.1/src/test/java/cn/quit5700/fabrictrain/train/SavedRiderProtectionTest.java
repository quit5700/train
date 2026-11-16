package cn.quit5700.fabrictrain.train;
import cn.quit5700.fabrictrain.offline.*;
import cn.quit5700.fabrictrain.route.TrackPoint;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SavedRiderProtectionTest {
 private CompoundTag player(String type,String id){var entity=new CompoundTag();entity.putString("id",type);entity.putString("TrainId",id);var root=new CompoundTag();root.put("Entity",entity);var player=new CompoundTag();player.put("RootVehicle",root);return player;}
 @Test void readsVanillaHostVehicleTrain(){var id=UUID.randomUUID();assertEquals(Set.of(id),SavedRiderProtection.fromPlayer(player("fabric_train_simulator:locomotive",id.toString())));}
 @Test void ignoresVanillaMinecartAndMalformedTrain(){assertTrue(SavedRiderProtection.fromPlayer(player("minecraft:minecart",UUID.randomUUID().toString())).isEmpty());assertTrue(SavedRiderProtection.fromPlayer(player("fabric_train_simulator:locomotive","broken")).isEmpty());}
 @Test void emptyOrWrongTypeRootCannotInventPassenger(){var p=new CompoundTag();assertTrue(SavedRiderProtection.fromPlayer(p).isEmpty());p.putString("RootVehicle","wrong");assertTrue(SavedRiderProtection.fromPlayer(p).isEmpty());}
 private OfflineTrainState state(UUID id){return new OfflineTrainState(id,1,TrainClass.CARGO,1000,0,0,0.3,false,0,1,0,0,0,20,List.of(new TrackPoint(0,80,0),new TrackPoint(0,80,1)),List.of());}
 @Test void protectsOnlyMatchingLiveManifestWithoutReplacingState(){var root=new InfrastructureData();var id=UUID.randomUUID();var other=UUID.randomUUID();var state=state(id);var member=UUID.randomUUID();root.put(new InfrastructureData.Owned(id,1,state,false,List.of(member)));root.put(new InfrastructureData.Owned(other,1,state(other),false,List.of(UUID.randomUUID())));SavedRiderProtection.protect(root,Set.of(id));var restored=root.trains.get(id);assertTrue(restored.occupied());assertFalse(restored.virtual());assertSame(state,restored.state());assertEquals(List.of(member),restored.members());assertFalse(root.trains.get(other).occupied());}
 @Test void cannotResurrectVirtualOrUnknownTrains(){var root=new InfrastructureData();var id=UUID.randomUUID();root.put(new InfrastructureData.Owned(id,1,state(id),true,List.of()));SavedRiderProtection.protect(root,Set.of(id,UUID.randomUUID()));assertEquals(1,root.trains.size());assertFalse(root.trains.get(id).occupied());assertTrue(root.trains.get(id).virtual());}
}
