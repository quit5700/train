package cn.quit5700.stationaccess;
import cn.quit5700.stationaccess.core.*;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.*;
import net.minecraft.network.*;


import net.minecraft.resources.RegistryOps;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import java.util.*;
public final class AccessNetwork {
 public record Request(String json) {public static final net.minecraft.resources.ResourceLocation ID=StationAccess.id("request");}
 public record Snapshot(String json) {public static final net.minecraft.resources.ResourceLocation ID=StationAccess.id("snapshot");}
 private static void snapshot(ServerPlayer player,String json){var buffer=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();buffer.writeUtf(json,1048576);ServerPlayNetworking.send(player,Snapshot.ID,buffer);}
 private static final Map<UUID,Selection> SELECTIONS=new HashMap<>();
 private static final Map<UUID,Session> SESSIONS=new HashMap<>();
 private static final Map<UUID,Integer> CLICKS=new HashMap<>();
 private static final class Selection { BlockPos a,b;String dimension,editing=""; }
 private static final class Session { String nonce=UUID.randomUUID().toString(),mode,message="",area="";InteractionHand hand;ItemStack tool;BlockPos terminal;int ticketSlot=-1,last=-100; }
 public static void register(){
  ServerPlayNetworking.registerGlobalReceiver(Request.ID,(server,player,handler,buffer,response)->{String json=buffer.readUtf(4096);server.execute(()->handle(player,json));});
  ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->areas(handler.player));
  ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{UUID id=handler.player.getUUID();SELECTIONS.remove(id);SESSIONS.remove(id);CLICKS.remove(id);});
 }
 public static void areas(ServerPlayer p){
  var list=AccessData.get(p.serverLevel().getServer()).state.areas.stream().filter(a->a.dimension.equals(AccessService.dimension(p.serverLevel()))).toList();
  for(int start=0;start<Math.max(1,list.size());start+=128){var out=new JsonObject();out.addProperty("mode","areas");out.addProperty("reset",start==0);var batch=new JsonArray();for(var a:list.subList(start,Math.min(start+128,list.size()))){var row=new JsonObject();row.addProperty("id",a.id);row.addProperty("owner",a.owner);row.addProperty("dimension",a.dimension);row.addProperty("shown",a.visible());row.addProperty("color",a.color);row.add("box",AccessData.GSON.toJsonTree(a.box));row.add("holes",AccessData.GSON.toJsonTree(a.holes));row.add("chunks",new JsonArray());batch.add(row);}out.add("areas",batch);snapshot(p,out.toString());}
 }
 public static void broadcast(MinecraftServer server){for(var p:server.getPlayerList().getPlayers()){areas(p);var session=SESSIONS.get(p.getUUID());if(session!=null&&session.mode.equals("setter")&&valid(p,session))send(p,session);}}
 public static void point(ServerPlayer p,InteractionHand hand,BlockPos pos){
  if(!p.getItemInHand(hand).is(StationAccess.SETTER)||!p.serverLevel().hasChunkAt(pos)||p.distanceToSqr(Vec3.atCenterOf(pos))>100)return;
  int now=p.serverLevel().getServer().getTickCount();if(now-CLICKS.getOrDefault(p.getUUID(),-10)<4)return;CLICKS.put(p.getUUID(),now);
  Selection selection=SELECTIONS.computeIfAbsent(p.getUUID(),id->new Selection());String dim=AccessService.dimension(p.serverLevel());if(selection.dimension==null||!selection.dimension.equals(dim)){selection.a=null;selection.b=null;selection.editing="";selection.dimension=dim;}
  if(selection.a==null||selection.b!=null){selection.a=pos.immutable();selection.b=null;AccessService.message(p,"first");}
  else {selection.b=pos.immutable();Session edit=new Session();edit.mode="setter";edit.hand=hand;edit.tool=p.getItemInHand(hand);edit.area=selection.editing;commit(p,edit,selection);if(SELECTIONS.containsKey(p.getUUID()))selection.b=null;if(!edit.message.isEmpty())AccessService.message(p,edit.message);}
  selectionSnapshot(p);
  p.serverLevel().sendParticles(p,ParticleTypes.END_ROD,false,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,6,.15,.15,.15,.01);
 }
 public static void openSetter(ServerPlayer p,InteractionHand hand){Session s=new Session();s.mode="setter";s.hand=hand;s.tool=p.getItemInHand(hand);if(!s.tool.is(StationAccess.SETTER))return;Selection selection=SELECTIONS.get(p.getUUID());if(selection!=null)s.area=selection.editing;SESSIONS.put(p.getUUID(),s);send(p,s);}
 public static void openTerminal(ServerPlayer p,BlockPos pos){if(!p.serverLevel().getBlockState(pos).is(StationAccess.TERMINAL))return;Session s=new Session();s.mode="terminal";s.terminal=pos.immutable();s.ticketSlot=Transactions.purchaseSlot(p);SESSIONS.put(p.getUUID(),s);send(p,s);}
 private static boolean valid(ServerPlayer p,Session s){return !p.isRemoved()&&(s.mode.equals("terminal")?s.terminal!=null&&p.serverLevel().hasChunkAt(s.terminal)&&p.serverLevel().getBlockState(s.terminal).is(StationAccess.TERMINAL)&&p.distanceToSqr(Vec3.atCenterOf(s.terminal))<=64:p.getItemInHand(s.hand)==s.tool&&s.tool.is(StationAccess.SETTER));}
 private static void send(ServerPlayer p,Session s){
  if(s.mode.equals("terminal"))s.ticketSlot=Transactions.purchaseSlot(p);
  var out=new JsonObject();out.addProperty("mode",s.mode);out.addProperty("nonce",s.nonce);out.addProperty("message",s.message);out.addProperty("selected",s.ticketSlot);out.addProperty("balance",AccessData.get(p.serverLevel().getServer()).state.wallets.balance(Ticket.id(s.ticketSlot>=0?p.getInventory().getItem(s.ticketSlot):Transactions.heldTicket(p))));
  var rows=new JsonArray();for(var a:AccessData.get(p.serverLevel().getServer()).state.areas){var row=new JsonObject();row.addProperty("id",a.id);row.addProperty("shown",a.visible());row.addProperty("color",a.color);row.addProperty("dimension",a.dimension);row.addProperty("manage",AccessService.manager(p,a));row.addProperty("names",stationNames(p,a));rows.add(row);}out.add("rows",rows);
  var selection=SELECTIONS.get(p.getUUID());if(selection!=null&&selection.a!=null&&selection.b!=null){out.add("box",AccessData.GSON.toJsonTree(Box.between(cell(selection.a),cell(selection.b))));}
  out.addProperty("editing",!s.area.isEmpty());out.addProperty("durability",s.tool==null?0:s.tool.getMaxDamage()-s.tool.getDamageValue());var inventory=new JsonArray();for(int i=0;i<36;i++)inventory.add(p.getInventory().getItem(i).save(new net.minecraft.nbt.CompoundTag()).toString());out.add("inventory",inventory);snapshot(p,out.toString());
 }
 private static Box.Cell cell(BlockPos p){return new Box.Cell(p.getX(),p.getY(),p.getZ());}
 private static void wear(ServerPlayer p,Session s){wearTool(p,s.hand,s.tool);}
 static void wearTool(ServerPlayer p,InteractionHand hand,ItemStack tool){if(!p.isCreative())tool.hurtAndBreak(1,p,user->user.broadcastBreakEvent(hand));}
 private static void handle(ServerPlayer p,String json){
  try{var request=JsonParser.parseString(json).getAsJsonObject();String early=request.get("op").getAsString();if(early.equals("breach")){breach(p,new BlockPos(request.get("x").getAsInt(),request.get("y").getAsInt(),request.get("z").getAsInt()));return;}if(early.equals("point")){var hand=InteractionHand.valueOf(request.get("hand").getAsString());point(p,hand,new BlockPos(request.get("x").getAsInt(),request.get("y").getAsInt(),request.get("z").getAsInt()));return;}Session s=SESSIONS.get(p.getUUID());if(s==null||!request.get("nonce").getAsString().equals(s.nonce)||!valid(p,s))return;String op=request.get("op").getAsString();if(op.equals("close")){SESSIONS.remove(p.getUUID());return;}
   int tick=p.serverLevel().getServer().getTickCount();if(tick-s.last<4)return;s.last=tick;AccessData data=AccessData.get(p.serverLevel().getServer());s.message="";
   if(s.mode.equals("terminal")){
    if(op.equals("select"))s.ticketSlot=Transactions.purchaseSlot(p);
    if(op.equals("charge")){var receipt=Transactions.purchaseDetailed(p,data);s.message=receipt.message();if(receipt.ticketSlot()>=0)s.ticketSlot=receipt.ticketSlot();}
   }else {
    if(request.has("area"))s.area=request.get("area").getAsString();
    Selection selection=SELECTIONS.get(p.getUUID());Area current=data.state.areas.stream().filter(a->a.id.equals(s.area)).findFirst().orElse(null);
    if(op.equals("clear")){SELECTIONS.remove(p.getUUID());s.message="cleared";}
    if(op.equals("reselect")&&current!=null&&AccessService.manager(p,current)){Selection next=new Selection();next.dimension=current.dimension;next.editing=current.id;Box.Cell fixed=current.anchor();next.a=new BlockPos(fixed.x(),fixed.y(),fixed.z());if(!next.dimension.equals(AccessService.dimension(p.serverLevel()))){s.message="dimension";}else{beginSecondPointEdit(p.getUUID(),next.a,next.dimension,next.editing);selectionSnapshot(p);AccessService.message(p,"reselect");var close=new JsonObject();close.addProperty("mode","selection_edit");snapshot(p,close.toString()); return;}}
    if(op.equals("visible")&&current!=null&&AccessService.manager(p,current)){current.shown=request.get("value").getAsBoolean();data.setDirty();broadcast(p.serverLevel().getServer());}
    if(op.equals("all_visible")){boolean value=request.get("value").getAsBoolean();for(var area:data.state.areas)if(AccessService.manager(p,area))area.shown=value;data.setDirty();broadcast(p.serverLevel().getServer());}
    if(op.equals("color")&&current!=null&&AccessService.manager(p,current)){int color=request.get("value").getAsInt();if(color>=0&&color<16){current.color=color;data.setDirty();broadcast(p.serverLevel().getServer());}}
    if(op.equals("delete")&&current!=null&&AccessService.manager(p,current)){data.state.areas.remove(current);data.setDirty();BoundaryWork.refresh();wear(p,s);SELECTIONS.remove(p.getUUID());s.area="";s.message="deleted";broadcast(p.serverLevel().getServer());}
    if(op.equals("create"))commit(p,s,selection);
   }send(p,s);
  }catch(RuntimeException invalid){org.slf4j.LoggerFactory.getLogger(StationAccess.ID).warn("Rejected invalid station access request: {}",invalid.toString());}
 }
 private static void commit(ServerPlayer p,Session s,Selection selection){AccessData data=AccessData.get(p.serverLevel().getServer());Area current=data.state.areas.stream().filter(a->a.id.equals(s.area)).findFirst().orElse(null);
     if(selection==null||selection.a==null||selection.b==null||!AccessService.dimension(p.serverLevel()).equals(selection.dimension)){s.message="two_points";return;}
     Box b=Box.between(cell(selection.a),cell(selection.b));if(!b.valid()||b.y0()<p.serverLevel().getMinBuildHeight()||b.y1()>=p.serverLevel().getMaxBuildHeight()){s.message="size";return;}
     if(!selection.editing.isEmpty()&&(current==null||!AccessService.manager(p,current))){s.message="owner";return;}
     if(data.state.areas.stream().anyMatch(a->a!=current&&a.dimension.equals(selection.dimension)&&a.box.overlaps(b))){s.message="overlap";return;}
     if(current!=null){data.state.areas.remove(current);}Area next=new Area(current==null?p.getUUID().toString():current.owner,selection.dimension,b);next.first=cell(selection.a);next.second=cell(selection.b);next.shown=current==null?true:current.visible();next.color=current==null?3:current.color;data.state.areas.add(next);data.setDirty();BoundaryWork.refresh();wear(p,s);s.area=next.id;SELECTIONS.remove(p.getUUID());s.message=b.large()?"created_large":"created";broadcast(p.serverLevel().getServer());
 }
 private static String stationNames(ServerPlayer p,Area area){return cn.quit5700.fabrictrain.data.RailwayDataManager.state(p.serverLevel()).devices().values().stream().filter(d->d.type()==cn.quit5700.fabrictrain.data.DeviceType.STATION&&d.dimension().equals(area.dimension)&&(area.box.contains(new Box.Cell(d.x(),d.y(),d.z()))||area.box.contains(new Box.Cell(d.x(),d.y()+1,d.z())))).sorted(java.util.Comparator.comparingLong(cn.quit5700.fabrictrain.data.RailwayDevice::id)).map(cn.quit5700.fabrictrain.data.RailwayDevice::name).collect(java.util.stream.Collectors.joining("   "));}
 private static void selectionSnapshot(ServerPlayer p){var out=new JsonObject();out.addProperty("mode","selection");var selection=SELECTIONS.get(p.getUUID());if(selection!=null&&selection.a!=null)out.add("first",AccessData.GSON.toJsonTree(cell(selection.a)));snapshot(p,out.toString());
 }
 static boolean breach(ServerPlayer p,BlockPos pos){if(!p.isCreative()||!p.serverLevel().hasChunkAt(pos)||p.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos))>36)return false;Area area=AccessService.at(p.serverLevel(),pos);if(area==null)return false;var state=p.serverLevel().getBlockState(pos);if(!state.isAir()&&!state.is(StationAccess.BOUNDARY))return false;var hit=p.serverLevel().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),Vec3.atCenterOf(pos),net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,p));if(hit.getType()!=net.minecraft.world.phys.HitResult.Type.MISS&&!hit.getBlockPos().equals(pos))return false;AccessService.openHole(p.serverLevel(),pos);return true;}
 static void beginSecondPointEdit(UUID player,BlockPos first,String dimension,String area){Selection next=new Selection();next.a=first.immutable();next.dimension=dimension;next.editing=area;SELECTIONS.put(player,next);SESSIONS.remove(player);}
 public static void clear(){SELECTIONS.clear();SESSIONS.clear();CLICKS.clear();}
}
