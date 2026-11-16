package cn.quit5700.qadiagnostic;

import net.fabricmc.api.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;import net.minecraft.world.phys.BlockHitResult;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.Gson;
import cn.quit5700.fabrictrain.registry.*;
import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.train.*;
import cn.quit5700.fabrictrain.entity.*;
import cn.quit5700.fabrictrain.block.entity.*;
import cn.quit5700.fabrictrain.depot.*;
import cn.quit5700.fabrictrain.network.*;
import cn.quit5700.fabrictrain.route.WorldLineValidator;
import cn.quit5700.fabrictrain.client.*;
import cn.quit5700.stationaccess.*;

public final class UiPauseProbe implements ModInitializer,ClientModInitializer {
 static final BlockPos START=new BlockPos(4096,79,4096),END=START.offset(0,0,512);
 static final int RIGHT_BUTTON=1;
 static final String[] CASES={"FreightYardScreen","DepotSettingsScreen","DeviceSettingsScreen","RailwayManagerScreen","LineSettingsScreen","TrainClassScreen","CargoSettingsScreen","FormationScreen","TrainListScreen","GeneratorScreen","AccessScreen","RangeListScreen","ordinary_chest","delete_confirmation"};
 static volatile boolean clientReady,done,trainReady,riding;
 static volatile long worldTime;static volatile double trainX,trainZ;static volatile int request=-1,served=-1;
 static volatile Object directPayload;static volatile int directCase=-1;static boolean confirmationClicked;static Screen confirmationParent;static int confirmationStable;static final List<Object> confirmationDebug=new ArrayList<>();
 static UUID trainId;static LocomotiveEntity head;static CargoCarEntity cargo;static long lineId;
 static int serverAge,phase,clientAge,active=-1,waiting,observed,gap;static long began;static double beganX,beganZ;
 static final Map<String,Boolean> checks=new LinkedHashMap<>();static final List<Object> measurements=new ArrayList<>();
 static void require(boolean value,String why){if(!value)throw new AssertionError(why);}
 static void check(String name,boolean value){checks.put(name,value);}
 static void teleport(ServerPlayer p,Vec3 v)throws Exception{
  java.lang.reflect.Method selected=null;
  for(var m:p.getClass().getMethods()){var t=m.getParameterTypes();if(t.length<6||t.length>8||t[0]!=ServerLevel.class||t[1]!=double.class||t[2]!=double.class||t[3]!=double.class)continue;boolean ok=true;for(int i=4;i<t.length;i++)ok&=t[i]==float.class||t[i]==boolean.class||Set.class.isAssignableFrom(t[i]);if(ok&&(selected==null||t.length>selected.getParameterCount()))selected=m;}
  if(selected==null)throw new NoSuchMethodException("player teleport");Object[] args=new Object[selected.getParameterCount()];args[0]=p.level();args[1]=v.x;args[2]=v.y;args[3]=v.z;var types=selected.getParameterTypes();for(int i=4;i<args.length;i++)args[i]=Set.class.isAssignableFrom(types[i])?Set.of():types[i]==float.class?0F:false;selected.invoke(p,args);
 }
 static boolean mount(ServerPlayer p,Entity e)throws Exception{
  java.lang.reflect.Method selected=null;for(var m:p.getClass().getMethods()){var t=m.getParameterTypes();if(m.getReturnType()!=boolean.class||t.length<2||t.length>3||t[0]!=Entity.class)continue;boolean ok=true;for(int i=1;i<t.length;i++)ok&=t[i]==boolean.class;if(ok&&(selected==null||t.length>selected.getParameterCount()))selected=m;}
  if(selected==null)throw new NoSuchMethodException("player mounting");Object[] args=new Object[selected.getParameterCount()];args[0]=e;for(int i=1;i<args.length;i++)args[i]=true;return (boolean)selected.invoke(p,args);
 }
 static Screen screen(Minecraft mc)throws Exception{
  for(var f:mc.getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())&&f.getType()==Screen.class)return (Screen)f.get(mc);
  var gui=mc.getClass().getField("gui").get(mc);return (Screen)gui.getClass().getMethod("screen").invoke(gui);
 }
 static void setup(ServerLevel w,ServerPlayer p)throws Exception{
  var data=RailwayDataManager.state(w);
  for(var line:new ArrayList<>(data.lines().values()))data.setLineStatus(line.id(),LineStatus.CLOSED);
  p.setGameMode(GameType.SURVIVAL);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setNoGravity(true);teleport(p,new Vec3(4099,82,4096));
  for(int z=-5;z<=512;z++){var pos=START.offset(0,1,z);w.getChunkAt(pos);w.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);w.setBlock(pos,ModBlocks.SIGNAL_RAIL.defaultBlockState().setValue(RailBlock.SHAPE,RailShape.NORTH_SOUTH),2);}
  w.setBlockAndUpdate(START,ModBlocks.TRAIN_DEPOT.defaultBlockState());var source=RailwayDataManager.register(w,START,DeviceType.DEPOT);
  w.setBlockAndUpdate(END,ModBlocks.TRAIN_DEPOT.defaultBlockState());var end=RailwayDataManager.register(w,END,DeviceType.DEPOT);
  var stop=START.offset(0,0,256);w.setBlockAndUpdate(stop,ModBlocks.STATION_MARKER.defaultBlockState());var station=RailwayDataManager.register(w,stop,DeviceType.STATION);
  var line=data.createEmptyLine("q5-ui-pause");lineId=line.id();data.bindDispatchDepot(source.id(),lineId);data.appendLineNode(lineId,station.id());data.appendLineNode(lineId,end.id());data.updateClassSpeedLimits(3.6,3.6,3.6);
  var depot=(TrainDepotBlockEntity)w.getBlockEntity(START);depot.setTemplate(DepotTemplate.decode("PC"));depot.configure(lineId,TrainClass.CARGO,3600,1,3.6,3.6,3.6);depot.setAutomaticDispatchEnabled(false);
  Container items=depot;items.setItem(0,new ItemStack(ModItems.LOCOMOTIVE));items.setItem(1,new ItemStack(ModItems.PASSENGER_CAR));items.setItem(2,new ItemStack(ModItems.CARGO_CAR));
  var gen=START.offset(2,1,0);w.setBlock(gen.below(),Blocks.STONE.defaultBlockState(),2);w.setBlock(START.offset(1,0,0),Blocks.STONE.defaultBlockState(),2);w.setBlock(START.offset(1,1,0),Blocks.REDSTONE_WIRE.defaultBlockState(),3);w.setBlockAndUpdate(gen,ModBlocks.POWER_GENERATOR.defaultBlockState());((Container)w.getBlockEntity(gen)).setItem(0,new ItemStack(Items.REDSTONE_BLOCK,64));
 }
 static void open(ServerLevel w,ServerPlayer p,int number)throws Exception{
  var data=RailwayDataManager.state(w);var pos=((Entity)head).blockPosition().offset(3,-1,0);w.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
  switch(number){
   case 0 -> {w.setBlockAndUpdate(pos,ModBlocks.FREIGHT_YARD.defaultBlockState());var d=RailwayDataManager.register(w,pos,DeviceType.FREIGHT_YARD);FreightNetworking.open(p,d.id(),false);}
   case 1 -> {DepotRemoteAccessSessions.authorize(p.getUUID(),RailwayDataManager.dimension(w),START,System.nanoTime());DepotNetworking.open(p,START,(TrainDepotBlockEntity)w.getBlockEntity(START));}
   case 2 -> {w.setBlockAndUpdate(pos,ModBlocks.STATION_MARKER.defaultBlockState());var d=RailwayDataManager.register(w,pos,DeviceType.STATION);DeviceSettingsNetworking.open(p,d);}
   case 3 -> RailwayManagerNetworking.open(p,lineId);
   case 4 -> {var line=data.line(lineId);directPayload=new LineSettingsNetworking.OpenPayload(lineId,line.name(),line.status().ordinal(),3600,3.6,1,line.nodeIds());directCase=number;}
   case 5 -> {TrainClassNetworking.authorizeSettings(p);directPayload=new TrainClassNetworking.OpenPayload(((Entity)head).getId(),TrainClass.CARGO.ordinal(),true,3.6,3.6,3.6,0,List.of());directCase=number;}
   case 6 -> {directPayload=new CargoSettingsNetworking.OpenPayload(((Entity)cargo).getId(),0,List.of());directCase=number;}
   case 7 -> FormationNetworking.open(p,head);
   case 8 -> {DepotRemoteAccessSessions.authorize(p.getUUID(),RailwayDataManager.dimension(w),START,System.nanoTime());var m=DepotNetworking.class.getDeclaredMethod("sendTrainList",ServerPlayer.class,BlockPos.class);m.setAccessible(true);m.invoke(null,p,START);}
   case 9 -> {w.setBlockAndUpdate(pos,ModBlocks.POWER_GENERATOR.defaultBlockState());p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.gameMode.useItemOn(p,w,ItemStack.EMPTY,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
   case 10 -> {w.setBlockAndUpdate(pos,StationAccess.TERMINAL.defaultBlockState());AccessNetwork.openTerminal(p,pos);}
   case 11 -> {p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(StationAccess.SETTER));AccessNetwork.openSetter(p,InteractionHand.MAIN_HAND);}
   case 12 -> {w.setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());((Container)w.getBlockEntity(pos)).setItem(0,new ItemStack(Items.DIAMOND,7));p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.gameMode.useItemOn(p,w,ItemStack.EMPTY,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
   case 13 -> RailwayManagerNetworking.open(p,0);
   default -> throw new AssertionError("unknown case");
  }
 }
 public void onInitialize(){ServerTickEvents.END_SERVER_TICK.register(server->{if(done||!clientReady||server.overworld().players().isEmpty())return;try{
  var w=server.overworld();var p=w.players().get(0);p.setNoGravity(true);p.fallDistance=0;serverAge++;
  if(phase==0){setup(w,p);phase=1;return;}
  if(phase==1&&serverAge>50){
   cn.quit5700.fabrictrain.power.RailPowerService.refreshNow(w);var data=RailwayDataManager.state(w);var line=data.line(lineId);var result=WorldLineValidator.validateForImmediateDispatchDetailed(w,START,line,data);require(result.status()==WorldLineValidator.ImmediateDispatchStatus.READY,"fixture route invalid "+result);data.setLineStatus(lineId,LineStatus.RUNNING);line=data.line(lineId);var depot=(TrainDepotBlockEntity)w.getBlockEntity(START);
   var generated=DepotDispatchService.dispatchConfiguredOutcome(w,START,line,depot,DepotDispatchConfig.saved(depot),true,ControlMode.ROUTE_AUTOMATIC);require(generated.succeeded(),"automatic train fixture failed "+generated.result());for(var e:generated.spawned()){if(e instanceof LocomotiveEntity l)head=l;if(e instanceof CargoCarEntity c)cargo=c;}require(head!=null&&cargo!=null,"actual formation missing");trainId=head.trainLink().trainId();require(mount(p,head),"rider could not board");phase=2;
  }
  if(phase==2){require(!((Entity)head).isRemoved()&&head.trainLink().trainId().equals(trainId),"original train replaced");riding=p.getVehicle()==head;worldTime=w.getGameTime();trainX=((Entity)head).getX();trainZ=((Entity)head).getZ();if(((Entity)head).getZ()>START.getZ()+1)trainReady=true;
   if(request>served){open(w,p,request);served=request;}
  }
 }catch(Throwable error){finish(error);}});}
 public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(mc->{if(done){mc.stop();return;}try{
  if(mc.player==null)return;var current=screen(mc);if(current==null)clientReady=true;if(!trainReady)return;clientAge++;
  if(gap>0){gap--;return;}
  if(active<0){active=0;request=0;waiting=0;observed=0;}
  if(directCase==active&&directPayload!=null){Object payload=directPayload;directPayload=null;directCase=-1;if(payload instanceof LineSettingsNetworking.OpenPayload p)mc.setScreen(new LineSettingsScreen(p));else if(payload instanceof TrainClassNetworking.OpenPayload p)mc.setScreen(new TrainClassScreen(p));else if(payload instanceof CargoSettingsNetworking.OpenPayload p)mc.setScreen(new CargoSettingsScreen(p));current=screen(mc);}
  if(active==13&&!confirmationClicked&&current instanceof RailwayManagerScreen){
   var selectedField=current.getClass().getDeclaredField("selected");selectedField.setAccessible(true);boolean selected=selectedField.get(current)!=null;
   if(confirmationDebug.size()<5)confirmationDebug.add(Map.of("served",served,"selected",selected,"same_parent",confirmationParent==current));
   if(served<13||selected){if(++waiting>160)throw new AssertionError("unselected manager snapshot not ready");confirmationStable=0;return;}
   if(confirmationParent!=current){confirmationParent=current;confirmationStable=0;}if(++confirmationStable<10)return;
   var layoutMethod=current.getClass().getDeclaredMethod("layout");layoutMethod.setAccessible(true);Object layout=layoutMethod.invoke(current);
   var mx=layout.getClass().getDeclaredMethod("middleX");mx.setAccessible(true);var ry=layout.getClass().getDeclaredMethod("rowsY");ry.setAccessible(true);double x=((Number)mx.invoke(layout)).doubleValue()+20,y=((Number)ry.invoke(layout)).doubleValue()+8;
   java.lang.reflect.Method click=null;Object[] args=null;
   for(var method:current.getClass().getDeclaredMethods()){var t=method.getParameterTypes();if(method.getReturnType()!=boolean.class)continue;
    if(t.length==3&&t[0]==double.class&&t[1]==double.class&&t[2]==int.class){method.invoke(current,x,y,RIGHT_BUTTON);if(screen(mc) instanceof net.minecraft.client.gui.screens.ConfirmScreen){click=method;args=null;break;}continue;}
    if(t.length==2&&t[1]==boolean.class){for(var c:t[0].getConstructors()){var ct=c.getParameterTypes();if(ct.length==3&&ct[0]==double.class&&ct[1]==double.class){Object info=ct[2].getConstructor(int.class,int.class).newInstance(RIGHT_BUTTON,0);click=method;args=new Object[]{c.newInstance(x,y,info),false};break;}}}
   }
   require(click!=null,"manager right click API missing");Object returned=args!=null?click.invoke(current,args):true;confirmationClicked=true;current=screen(mc);confirmationDebug.add(Map.of("method",click.toString(),"returned",String.valueOf(returned),"after",current==null?"none":current.getClass().getName(),"x",x,"y",y));
  }
  boolean matches=current!=null&&(active==12?current.getClass()==net.minecraft.client.gui.screens.inventory.ContainerScreen.class:active==13?current instanceof net.minecraft.client.gui.screens.ConfirmScreen:current.getClass().getSimpleName().equals(CASES[active]));
  if(!matches){if(++waiting>160)throw new AssertionError("screen did not remain open "+CASES[active]+" actual="+(current==null?"none":current.getClass().getName()));return;}
  if(observed==0){began=worldTime;beganX=trainX;beganZ=trainZ;for(var child:current.children())if(child instanceof EditBox edit){edit.setFocused(true);break;}}
  observed++;if(observed<80)return;
  long elapsed=worldTime-began;double distance=Math.hypot(trainX-beganX,trainZ-beganZ);String key=CASES[active];boolean clocks=elapsed>=30,moving=distance>.5;
  check(key+"_non_pausing",!current.isPauseScreen());check(key+"_world_advances",clocks);check(key+"_original_train_moves",moving);check(key+"_rider_stays",riding);
  measurements.add(Map.of("screen",key,"client_ticks",observed,"world_ticks",elapsed,"train_distance",distance,"riding",riding,"pause_flag",current.isPauseScreen()));
  write(false,null);
  if(!clocks||!moving||!riding||current.isPauseScreen()){finish(null);return;}
  current.onClose();active++;if(active==CASES.length){finish(null);return;}observed=0;waiting=0;request=active;gap=10;
 }catch(Throwable error){finish(error);mc.stop();}});}
 static synchronized void write(boolean complete,Throwable error){try{var output=new LinkedHashMap<String,Object>();output.put("complete",complete);output.put("passed",complete&&error==null&&!checks.containsValue(false));output.put("ticks",serverAge);output.put("client_ticks",clientAge);output.put("checks",checks);output.put("measurements",measurements);output.put("confirmation_debug",confirmationDebug);output.put("original_train",trainId==null?"":trainId.toString());output.put("error",error==null?"":error.toString());Files.writeString(Path.of(System.getProperty("q5.full.result")),new Gson().toJson(output),StandardCharsets.UTF_8);}catch(Exception failure){throw new RuntimeException(failure);}}
 static synchronized void finish(Throwable error){if(done)return;if(error!=null)error.printStackTrace();write(true,error);done=true;}
}
