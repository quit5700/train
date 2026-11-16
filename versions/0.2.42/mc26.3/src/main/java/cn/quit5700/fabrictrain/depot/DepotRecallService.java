package cn.quit5700.fabrictrain.depot;
import cn.quit5700.fabrictrain.block.entity.TrainDepotBlockEntity;
import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.train.*;
import cn.quit5700.fabrictrain.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import java.util.*;
/** Server-owned requests survive reload; unloaded partial formations are never guessed or deleted. */
public final class DepotRecallService {
 public static void request(ServerLevel level,BlockPos pos,TrainDepotBlockEntity depot,long origin){
  var data=RailwayDataManager.state(level);data.presentTrainsFromDepot(origin).values().stream().filter(r->r.controlMode()==ControlMode.ROUTE_AUTOMATIC).forEach(r->depot.pendingRecalls().add(r.trainId()));
  for(var record:data.trainRecords().values()){var manifest=data.trainManifest(record.trainId());if(record.controlMode()==ControlMode.ROUTE_AUTOMATIC&&record.presence()!=TrainPresence.DESTROYED&&manifest!=null&&manifest.originDepotNumber()==origin&&manifest.totalVehicles()>0)depot.pendingRecalls().add(record.trainId());}depot.setChanged();process(level,depot);
 }
 public static void process(ServerLevel level,TrainDepotBlockEntity depot){
  if(depot.pendingRecalls().isEmpty())return;
  var data=RailwayDataManager.state(level);var offline=OfflineTrainService.state(level.getServer());
  for(UUID id:new ArrayList<>(depot.pendingRecalls())){
   var known=data.trainRecord(id);
   if(known!=null&&known.controlMode()!=ControlMode.ROUTE_AUTOMATIC){depot.pendingRecalls().remove(id);depot.setChanged();continue;}

   OfflineTrainState state=offline.get(id);
   if(state!=null){
    var items=new ArrayList<ItemStack>();items.add(new ItemStack(ModItems.LOCOMOTIVE));
    for(var car:state.cars()){car.items().stream().map(ItemStack::copy).forEach(items::add);items.add(new ItemStack(car.type()==CarType.CARGO?ModItems.CARGO_CAR:ModItems.PASSENGER_CAR));}
    if(DepotRecoveryService.recoverItems(level,depot,items)){TrainControllerManager.retireTrainResources(level,id);depot.pendingRecalls().remove(id);depot.setChanged();}continue;
   }
   var cars=new ArrayList<Entity>();for(Entity e:level.getAllEntities())if(e instanceof LinkedTrainCar linked&&linked.trainLink().trainId().equals(id)&&!e.isRemoved())cars.add(e);
   var manifest=data.trainManifest(id);var record=data.trainRecord(id);
   if(cars.isEmpty()&&((record==null&&manifest==null)||record!=null&&record.presence()==TrainPresence.DESTROYED)){depot.pendingRecalls().remove(id);depot.setChanged();continue;}
   if(manifest!=null&&cars.size()<manifest.totalVehicles())continue;
   if(!cars.isEmpty()&&DepotRecoveryService.recover(cars,depot)){depot.pendingRecalls().remove(id);depot.setChanged();}
  }
 }
}
