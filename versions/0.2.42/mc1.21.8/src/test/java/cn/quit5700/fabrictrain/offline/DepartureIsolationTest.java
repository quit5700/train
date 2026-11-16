package cn.quit5700.fabrictrain.offline;
import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.train.*;
import cn.quit5700.fabrictrain.route.TrackPoint;
import net.minecraft.nbt.NbtOps;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DepartureIsolationTest {
 private RailwayLine line(List<Long> nodes,LineStatus status){return new RailwayLine(1,"original",2,nodes,5,80,2,status,Map.of(),Map.of());}
 private OfflineTrainState state(UUID id){return new OfflineTrainState(id,1,TrainClass.CARGO,80,0,.25,.4,false,0,1,0,0,0,100,List.of(new TrackPoint(1,64,1),new TrackPoint(1,64,2)),List.of());}
 @Test void changedRouteDoesNotChangeDispatchedNodes(){
  var departure=TrainDeparture.from("2|[3, 4]|{}|{}",List.of());assertNotNull(departure);
  var operating=departure.operatingLine(line(List.of(5L,6L),LineStatus.NEEDS_VALIDATION));
  assertEquals(List.of(3L,4L),operating.nodeIds());assertEquals(LineStatus.RUNNING,operating.status());
  assertEquals(LineStatus.MAINTENANCE,departure.operatingLine(line(List.of(5L,6L),LineStatus.MAINTENANCE)).status());
 }
 @Test void routeSnapshotAndPassengerFlagSurviveOwnershipTransitions(){
  var root=new InfrastructureData();var id=UUID.randomUUID();var member=UUID.randomUUID();var trip=state(id);
  var departure=TrainDeparture.from("2|[3, 4]|{}|{}",List.of());
  root.put(new InfrastructureData.Owned(id,2,trip,false,List.of(member),true,Optional.of(departure)));
  root.put(new InfrastructureData.Owned(id,2,trip,false,List.of(member),true));
  var value=root.trains.get(id);assertTrue(value.occupied());assertEquals(departure,value.departure().orElseThrow());assertEquals(.25,value.state().segmentProgress());
  var encoded=InfrastructureData.CODEC.encodeStart(NbtOps.INSTANCE,root).result().orElseThrow();
  var restored=InfrastructureData.CODEC.parse(NbtOps.INSTANCE,encoded).result().orElseThrow();
  assertEquals(value,restored.trains.get(id));
 }
 @Test void legacySnapshotOnlyAttachesToExactMatchingPath(){
  var root=new InfrastructureData();var id=UUID.randomUUID();var pos=new InfrastructureData.Pos("minecraft:overworld",1,63,1);
  var cfg=new InfrastructureData.Config(1,"C","CARGO",5,2,true,true,80);
  var plan=new InfrastructureData.Plan(state(id).path(),false,0,List.of(),"2|[3, 4]|{}|{}");
  root.put(new InfrastructureData.Depot(pos,2,cfg,List.of(pos),Optional.of(plan),0,0,List.of()));
  root.put(new InfrastructureData.Owned(id,2,state(id),true,List.of()));assertTrue(root.trains.get(id).departure().isPresent());
  var other=UUID.randomUUID();var s=state(other);var wrong=new OfflineTrainState(other,1,TrainClass.CARGO,80,0,0,0,false,0,1,0,0,0,100,List.of(new TrackPoint(8,64,1),new TrackPoint(8,64,2)),List.of());
  root.put(new InfrastructureData.Owned(other,2,wrong,true,List.of()));assertTrue(root.trains.get(other).departure().isEmpty());
 }
 @Test void electricalWireFaultPersistsSeparatelyFromAnyLine(){var root=new InfrastructureData();var p=new InfrastructureData.Pos("minecraft:overworld",1,64,1);var feed=new InfrastructureData.Feed("generator->rail",List.of(p),Set.of(p),new HashSet<>(Set.of(p)));root.feeds.put(feed.key(),feed);root.railFaults.add(p);var encoded=InfrastructureData.CODEC.encodeStart(NbtOps.INSTANCE,root).result().orElseThrow();var restored=InfrastructureData.CODEC.parse(NbtOps.INSTANCE,encoded).result().orElseThrow();assertEquals(Set.of(p),restored.feeds.get(feed.key()).faults());assertEquals(Set.of(p),restored.railFaults);assertTrue(restored.trains.isEmpty());}
}
