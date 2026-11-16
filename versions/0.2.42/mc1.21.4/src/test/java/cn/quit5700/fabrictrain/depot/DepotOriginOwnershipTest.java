package cn.quit5700.fabrictrain.depot;
import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.train.ControlMode;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class DepotOriginOwnershipTest {
 @Test void changingOrDeletingLineDoesNotHideOriginTrains(){
  var d=new RailwaySavedData();var a=d.register(DeviceType.DEPOT,"minecraft:overworld",0,0,0);var b=d.register(DeviceType.DEPOT,"minecraft:overworld",10,0,0);
  var old=d.createLine(a.id());UUID auto=UUID.randomUUID(),manual=UUID.randomUUID(),other=UUID.randomUUID();
  d.ensureTrain(auto,true,old.id(),ControlMode.ROUTE_AUTOMATIC);d.ensureTrain(manual,true,0L,ControlMode.MANUAL);d.ensureTrain(other,true,0L,ControlMode.MANUAL);
  for(UUID id:List.of(auto,manual,other))d.replaceTrainManifest(id,id.equals(other)?b.id():a.id(),List.of(new TrainMember(UUID.randomUUID(),TrainVehicleKind.LOCOMOTIVE,1)));
  assertEquals(Set.of(auto,manual),d.presentTrainsFromDepot(a.id()).keySet());
  d.createEmptyLine("new-line");d.deleteLine(old.id());
  assertEquals(Set.of(auto,manual),d.presentTrainsFromDepot(a.id()).keySet());assertFalse(d.presentTrainsFromDepot(a.id()).containsKey(other));
  d.retireTrain(auto);assertEquals(Set.of(manual),d.presentTrainsFromDepot(a.id()).keySet());
 }
}
