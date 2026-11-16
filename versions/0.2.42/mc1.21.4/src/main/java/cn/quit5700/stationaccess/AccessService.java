package cn.quit5700.stationaccess;
import cn.quit5700.stationaccess.core.*;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraft.network.chat.Component;

import java.util.*;
public final class AccessService {
 public static final int WORK_BUDGET=128;
 private static final Map<UUID,Previous> PREVIOUS=new HashMap<>();
 private static final Map<UUID,Integer> WARNED=new HashMap<>();
 private record Previous(String dimension,Vec3 position){}
 public static String dimension(ServerLevel w){return w.dimension().location().toString();}
 public static Box.Point point(Vec3 p){return new Box.Point(p.x,p.y,p.z);}
 public static Area at(ServerLevel w,BlockPos p){for(var a:AccessData.get(w.getServer()).state.areas)if(a.dimension.equals(dimension(w))&&a.covers(p))return a;return null;}
 public static boolean protectedCell(net.minecraft.world.level.Level level,BlockPos p){return level instanceof ServerLevel w&&at(w,p)!=null;}
 public static boolean manager(ServerPlayer p,Area a){return a.owner.equals(p.getUUID().toString())||p.hasPermissions(2);}
 public static void openHole(net.minecraft.world.level.Level level,BlockPos pos){if(level instanceof ServerLevel w){Area a=at(w,pos);if(a!=null){a.holes.add(pos.asLong());AccessData.get(w.getServer()).setDirty();AccessNetwork.broadcast(w.getServer());}}}
 public static boolean trusted(Entity e){return Rules.trusted(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString())||e instanceof ItemEntity item&&Rules.trusted(BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).toString());}
 public static boolean canCross(Entity e,Area area){
  if(e.isSpectator())return true;
  if(e instanceof ServerPlayer p){if(trainPassenger(p)||area.box.contains(point(p.position())))return true;var data=AccessData.get(p.level().getServer());return !Ticket.find(p,data,true).isEmpty();}
  if(!trusted(e))return false;
  if(e instanceof cn.quit5700.fabrictrain.train.LinkedTrainCar)return true;
  for(var rider:e.getIndirectPassengers())if(rider instanceof ServerPlayer&&!canCross(rider,area))return false;
  return true;
 }
 public static void message(ServerPlayer p,String key){p.displayClientMessage(Component.translatable("message.station_access."+key),true);}
 public static void tick(MinecraftServer server){
  AccessData data=AccessData.get(server);BoundaryWork.tick(server,data);
  var present=new HashSet<UUID>();
  for(ServerPlayer p:server.getPlayerList().getPlayers()){
   present.add(p.getUUID());audit(p,data);TicketHud.tick(p,data);
   if(server.getTickCount()%10==0){for(int slot=0;slot<p.getInventory().getContainerSize();slot++)Ticket.refresh(p.getInventory().getItem(slot),data);p.inventoryMenu.broadcastChanges();outline(p,data);}
  }
  if(!data.state.areas.isEmpty())for(ServerLevel level:server.getAllLevels())for(Entity e:level.getAllEntities())if(!(e instanceof ServerPlayer)&&!e.isRemoved()){present.add(e.getUUID());auditVehicle(e,data);}
  TicketHud.retain(present);PREVIOUS.keySet().retainAll(present);WARNED.keySet().retainAll(present);
 }
 static boolean trainPassenger(ServerPlayer p){return p.isPassenger()&&p.getRootVehicle() instanceof cn.quit5700.fabrictrain.train.LinkedTrainCar;}
 static void audit(ServerPlayer p,AccessData data){
  String dim=dimension(p.serverLevel());Vec3 now=p.position();Previous before=PREVIOUS.get(p.getUUID());
  if(p.isSpectator()){PREVIOUS.put(p.getUUID(),new Previous(dim,now));return;}
  Set<String> admitted=data.state.admissions.computeIfAbsent(p.getUUID().toString(),k->new HashSet<>());
  boolean fresh=before==null||!before.dimension.equals(dim);
  if(fresh&&before!=null)admitted.clear();
  var entered=new ArrayList<Area>();var exiting=new ArrayList<Area>();
  for(Area a:data.state.areas){if(!a.dimension.equals(dim))continue;
   boolean inside=a.box.contains(point(now));
   boolean core=a.box.inner().contains(point(now))||!fresh&&!a.box.inner().crossings(point(before.position),point(now)).isEmpty();
   boolean was=admitted.contains(a.id);
   var crossings=fresh?java.util.List.<Box.Cell>of():a.box.crossings(point(before.position),point(now));
   boolean fromInside=!fresh&&a.box.contains(point(before.position));
   boolean entering=!was&&!fromInside&&!crossings.isEmpty();
   boolean closedEntry=entering&&a.closed(crossings.get(0));
   if(closedEntry&&!trainPassenger(p)&&Ticket.find(p,data,true).isEmpty()){reject(p,before.position,data);return;}
   // A fresh login is not a boundary crossing. Existing saved admissions are preserved.
   if(core&&(inside||!crossings.isEmpty()))entered.add(a);
   if(!fresh&&!inside&&(was||core||fromInside&&a.box.inner().contains(point(before.position))))exiting.add(a);
  }
  boolean changed=false;
  for(Area a:entered)changed|=admitted.add(a.id);
  for(Area a:exiting){
   ItemStack ticket=Ticket.find(p,data,true);
   if(!ticket.isEmpty()&&data.state.wallets.charge(Ticket.id(ticket))){Ticket.refresh(ticket,data);message(p,"charged");changed=true;}
   changed|=admitted.remove(a.id);
  }
  changed|=admitted.removeIf(id->data.state.areas.stream().noneMatch(a->a.id.equals(id)&&a.dimension.equals(dim)));
  if(changed)data.setDirty();
  PREVIOUS.put(p.getUUID(),new Previous(dim,now));
 }
 private static Vec3 outside(ServerPlayer p,Area a){
  var candidates=new ArrayList<Vec3>();for(int dy=0;dy<16;dy++)for(int side=0;side<4;side++)candidates.add(new Vec3(side==0?a.box.x0()-1.5:side==1?a.box.x1()+2.5:Math.max(a.box.x0(),Math.min(p.getX(),a.box.x1())),p.getY()+dy,side==2?a.box.z0()-1.5:side==3?a.box.z1()+2.5:Math.max(a.box.z0(),Math.min(p.getZ(),a.box.z1()))));
  candidates.sort(Comparator.comparingDouble(v->v.distanceToSqr(p.position())));
  for(Vec3 v:candidates)if(p.serverLevel().hasChunkAt(BlockPos.containing(v))&&p.serverLevel().noCollision(p,p.getBoundingBox().move(v.subtract(p.position()))))return v;
  return Vec3.atCenterOf(p.serverLevel().getSharedSpawnPos()).add(0,1,0);
 }
 private static void reject(ServerPlayer p,Vec3 to,AccessData data){if(p.isPassenger()){Entity vehicle=p.getRootVehicle();Previous old=PREVIOUS.get(vehicle.getUUID());if(old!=null)vehicle.teleportTo(old.position.x,old.position.y,old.position.z);p.stopRiding();}p.teleportTo(to.x,to.y,to.z);p.setDeltaMovement(Vec3.ZERO);PREVIOUS.put(p.getUUID(),new Previous(dimension(p.serverLevel()),to));int n=p.serverLevel().getServer().getTickCount();if(n-WARNED.getOrDefault(p.getUUID(),-40)>=40){WARNED.put(p.getUUID(),n);message(p,"denied");}}
 static void auditVehicle(Entity e,AccessData data){String dim=dimension((ServerLevel)e.level());Previous before=PREVIOUS.get(e.getUUID());Vec3 now=e.position();if(before!=null&&before.dimension.equals(dim))for(Area a:data.state.areas)if(a.dimension.equals(dim)&&a.box.crossings(point(before.position),point(now)).stream().anyMatch(a::closed)){
   boolean collision=now.distanceToSqr(before.position)>256||e.level().getBlockCollisions(e,e.getBoundingBox().deflate(.01).expandTowards(before.position.subtract(now))).iterator().hasNext();
   if(!canCross(e,a)||collision){e.teleportTo(before.position.x,before.position.y,before.position.z);e.setDeltaMovement(Vec3.ZERO);now=e.position();break;}
  }PREVIOUS.put(e.getUUID(),new Previous(dim,now));}
 private static void outline(ServerPlayer p,AccessData data){/* Virtual frames are drawn client-side with the shared visibility/color state. */}
 public static void clear(){TicketHud.clear();PREVIOUS.clear();WARNED.clear();}
}
