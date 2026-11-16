package cn.quit5700.fabrictrain.train;
import cn.quit5700.fabrictrain.offline.InfrastructureData;
import cn.quit5700.fabrictrain.route.TrackPoint;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class OccupiedTrainPersistenceTest {
 @Test void occupiedCheckpointRetainsOriginalMembersAcrossReload(){
  var id=UUID.randomUUID();var member=UUID.randomUUID();var state=new OfflineTrainState(id,1,TrainClass.CARGO,1000,0,0,0.3,false,0,1,0,0,0,20,List.of(new TrackPoint(0,80,0),new TrackPoint(0,80,1)),List.of());
  var data=new InfrastructureData();data.put(new InfrastructureData.Owned(id,1,state,false,List.of(member),true));
  var tag=InfrastructureData.CODEC.encodeStart(NbtOps.INSTANCE,data).result().orElseThrow();var loaded=InfrastructureData.CODEC.parse(NbtOps.INSTANCE,tag).result().orElseThrow().trains.get(id);
  assertTrue(loaded.occupied());assertFalse(loaded.virtual());assertEquals(List.of(member),loaded.members());assertEquals(0.3,loaded.state().speed());
 }
 @Test void legacyUnoccupiedConstructorRemainsCompatible(){var id=UUID.randomUUID();var state=new OfflineTrainState(id,1,TrainClass.CARGO,1000,0,0,0,false,0,1,0,0,0,20,List.of(new TrackPoint(0,80,0),new TrackPoint(0,80,1)),List.of());assertFalse(new InfrastructureData.Owned(id,1,state,false,List.of()).occupied());}
}
