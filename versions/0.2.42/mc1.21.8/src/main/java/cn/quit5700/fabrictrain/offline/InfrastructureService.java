package cn.quit5700.fabrictrain.offline;

import static cn.quit5700.fabrictrain.offline.InfrastructureData.*;
import cn.quit5700.fabrictrain.block.entity.*;
import cn.quit5700.fabrictrain.cargo.*;
import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.depot.*;
import cn.quit5700.fabrictrain.entity.*;
import cn.quit5700.fabrictrain.power.RailPowerService;
import cn.quit5700.fabrictrain.registry.*;
import cn.quit5700.fabrictrain.route.*;
import cn.quit5700.fabrictrain.train.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import java.util.*;

public final class InfrastructureService {
 private InfrastructureService(){}
 private static final Map<MinecraftServer,Map<String,Container>> BOUND=new WeakHashMap<>();
 private static final Map<MinecraftServer,Map<String,Set<String>>> RAIL_INDEX=new WeakHashMap<>();
 private static final Set<MinecraftServer> STOPPING=Collections.newSetFromMap(new WeakHashMap<>());
 public static InfrastructureData data(MinecraftServer s){return s.overworld().getDataStorage().computeIfAbsent(InfrastructureData.TYPE);}
 public static Pos pos(ServerLevel w,BlockPos p){return new Pos(RailwayDataManager.dimension(w),p.getX(),p.getY(),p.getZ());}
 public static boolean stopping(MinecraftServer s){return STOPPING.contains(s);}
 public static void stop(MinecraftServer s){
  finish(s);var root=data(s);var bound=BOUND.getOrDefault(s,Map.of());
  for(var w:s.getAllLevels())for(var entry:new ArrayList<>(bound.entrySet())){
   var stock=root.stocks.get(entry.getKey());var container=entry.getValue();
   if(stock==null||!stock.pos().dimension().equals(RailwayDataManager.dimension(w)))continue;
   if(container instanceof PowerGeneratorBlockEntity generator)captureGenerator(w,stock.pos().block(),generator,true);
   else captureStock(w,stock.pos().block(),container,true);
  }
  STOPPING.add(s);
 }
 public static void forget(ServerLevel w,BlockPos p){var rails=RAIL_INDEX.get(w.getServer());if(rails!=null&&rails.containsKey(pos(w,p).key()))cn.quit5700.fabrictrain.power.SavedPowerPlanBridge.invalidate(w);var root=data(w.getServer());var key=pos(w,p).key();root.stocks.remove(key);root.depots.remove(key);root.generators.remove(key);root.setDirty();BOUND.computeIfAbsent(w.getServer(),s->new HashMap<>()).remove(key);}
 public static void clear(MinecraftServer s){BOUND.remove(s);RAIL_INDEX.remove(s);STOPPING.remove(s);}
 private static String block(ServerLevel w,BlockPos p){return BuiltInRegistries.BLOCK.getKey(LoadedRailWorld.state(w,p).getBlock()).toString();}
 public static Depot depot(ServerLevel w,long id){return data(w.getServer()).depots.values().stream().filter(d->d.id()==id&&d.pos().dimension().equals(RailwayDataManager.dimension(w))).findFirst().orElse(null);}
 private static String signature(RailwayLine l){return l.depotId()+"|"+l.nodeIds()+"|"+new TreeMap<>(l.switchDirections())+"|"+new TreeMap<>(l.stationStopTracks());}
 private static Stock copy(ServerLevel w,BlockPos p,Container c,boolean owned){var slots=new ArrayList<Slot>();for(int i=0;i<c.getContainerSize();i++)if(!c.getItem(i).isEmpty())slots.add(new Slot(i,c.getItem(i)));String kind=c instanceof BlockEntity be?BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).toString():block(w,p);return new Stock(pos(w,p),kind,c.getContainerSize(),c.getMaxStackSize(),slots,owned);}
 private static ArrayList<ItemStack> items(Stock s){var r=new ArrayList<ItemStack>(s.size());for(int i=0;i<s.size();i++)r.add(ItemStack.EMPTY);for(var v:s.slots())if(v.slot()>=0&&v.slot()<r.size())r.set(v.slot(),v.stack().copy());return r;}
 private static void write(Container c,List<ItemStack> values){for(int i=0;i<c.getContainerSize();i++)c.setItem(i,i<values.size()?values.get(i).copy():ItemStack.EMPTY);c.setChanged();}
 public static void bind(ServerLevel w,BlockPos p,Container c){
  var root=data(w.getServer());var key=pos(w,p).key();var bound=BOUND.computeIfAbsent(w.getServer(),s->new HashMap<>());if(bound.get(key)==c)return;
  var saved=root.stocks.get(key);
  if(saved!=null&&saved.owned()&&saved.size()==c.getContainerSize()&&saved.block().equals(block(w,p)))write(c,items(saved));
  bound.put(key,c);
  var fuel=root.generators.get(key);if(c instanceof PowerGeneratorBlockEntity g&&fuel!=null&&fuel.owned())g.restoreOfflineFuel(fuel.until(),fuel.reserved(),w.getGameTime());
 }
 private static void captureStock(ServerLevel w,BlockPos p,Container c,boolean own){bind(w,p,c);var root=data(w.getServer());var old=root.stocks.get(pos(w,p).key());root.put(copy(w,p,c,own||old!=null&&old.owned()));}
 private static List<Pos> captureStores(ServerLevel w,TrainDepotBlockEntity c,boolean own){var list=new ArrayList<Pos>();captureStock(w,c.getBlockPos(),c,own);list.add(pos(w,c.getBlockPos()));for(var direction:Direction.values()){var p=c.getBlockPos().relative(direction);if(LoadedRailWorld.entity(w,p) instanceof Container nearby){captureStock(w,p,nearby,own);list.add(pos(w,p));}}return list;}
 private static void captureDepot(ServerLevel w,RailwayDevice device,TrainDepotBlockEntity c,boolean own){
  var root=data(w.getServer());var p=pos(w,c.getBlockPos());var old=root.depots.get(p.key());var stores=captureStores(w,c,own);
  var cfg=new Config(c.selectedLineId(),c.template().encode(),c.trainClass().name(),c.dispatchIntervalSeconds(),c.maximumTrains(),c.automaticDispatchEnabled(),c.creativeAutomaticDispatch(),c.speedLimitKmh(c.trainClass()));
  root.put(new Depot(p,device.id(),cfg,stores,old==null?Optional.empty():old.plan(),old==null?Long.MIN_VALUE/2:old.lastDispatch(),c.lastAutomaticDispatchResultOrdinal(),old==null?List.of():old.overflow()));
 }
 private static void captureGenerator(ServerLevel w,BlockPos p,PowerGeneratorBlockEntity g,boolean own){captureStock(w,p,g,own);var root=data(w.getServer());var old=root.generators.get(pos(w,p).key());root.put(new Generator(pos(w,p),g.offlineFuelUntil(),g.offlineReservedSlot(),own||old!=null&&old.owned()));}
 // CHUNK_LOAD runs before the full chunk future completes: setChanged here would wait on itself.
 // Binding is performed on the server tick, or immediately before an owned inventory transaction.
 public static void chunkLoad(ServerLevel w,LevelChunk chunk){
  var root=data(w.getServer());var bound=BOUND.computeIfAbsent(w.getServer(),server->new HashMap<>());
  for(var e:chunk.getBlockEntities().entrySet()){
   var p=e.getKey();var key=pos(w,p).key();var stock=root.stocks.get(key);var entity=e.getValue();
   if(stock==null||!stock.owned()||!stock.block().equals(BuiltInRegistries.BLOCK.getKey(chunk.getBlockState(p).getBlock()).toString()))continue;
   if(!(entity instanceof cn.quit5700.fabrictrain.mixin.ManagedInventoryAccessor access)||!(entity instanceof Container container)||bound.get(key)==container)continue;
   var previous=bound.get(key);
   if(previous!=null){
    if(previous instanceof PowerGeneratorBlockEntity generator)captureGenerator(w,p,generator,true);
    else captureStock(w,p,previous,true);
    stock=root.stocks.get(key);
   }
   var target=access.fabricTrain$items();if(target.size()!=stock.size())continue;
   var values=items(stock);for(int i=0;i<target.size();i++)target.set(i,values.get(i));
   var fuel=root.generators.get(key);
   if(container instanceof PowerGeneratorBlockEntity generator&&fuel!=null&&fuel.owned())
    generator.restoreOfflineFuelDuringChunkLoad(fuel.until(),fuel.reserved(),w.getGameTime());
   bound.put(key,container);
   cn.quit5700.fabrictrain.compat.ChunkDirtyCompat.mark(chunk);
  }
 }
 public static void chunkUnload(ServerLevel w,LevelChunk chunk){
  var root=data(w.getServer());for(var e:chunk.getBlockEntities().entrySet()){
   var p=e.getKey();var key=pos(w,p).key();var be=e.getValue();
   boolean authoritative=!stopping(w.getServer())&&BOUND.getOrDefault(w.getServer(),Map.of()).get(key)==be;
   if(authoritative&&be instanceof Container c&&root.stocks.containsKey(key))root.put(copy(w,p,c,true));
   if(authoritative&&be instanceof PowerGeneratorBlockEntity g&&root.generators.containsKey(key))root.put(new Generator(pos(w,p),g.offlineFuelUntil(),g.offlineReservedSlot(),true));
   BOUND.computeIfAbsent(w.getServer(),s->new HashMap<>()).remove(key,be);
  }
 }
 public static void cacheValidatedPlan(ServerLevel w,BlockPos origin,RailwayLine line,DepotDispatchConfig cfg){
  var data=RailwayDataManager.state(w);var access=cn.quit5700.fabrictrain.route.WorldLineValidator.validateDispatchAccess(w,origin,line,data);
  var path=cn.quit5700.fabrictrain.route.RuntimePathCache.get(line.id());if(access.path().isEmpty()||path.isEmpty())return;
  var complete=new ArrayList<TrackPoint>(access.path());if(complete.get(complete.size()-1).equals(path.get(0)))complete.remove(complete.size()-1);
  int lineStart=complete.size();complete.addAll(path);boolean loop=cn.quit5700.fabrictrain.route.LineLoopRules.isLoop(line.nodeIds(),id->{var device=data.device(id);return device==null?null:device.type();});
  observePlan(w,origin,cfg,List.copyOf(complete),loop,lineStart,false);
 }
 public static void observePlan(ServerLevel w,BlockPos origin,DepotDispatchConfig cfg,List<TrackPoint> path,boolean loop,int lineStart){observePlan(w,origin,cfg,path,loop,lineStart,true);}
 private static void observePlan(ServerLevel w,BlockPos origin,DepotDispatchConfig cfg,List<TrackPoint> path,boolean loop,int lineStart,boolean dispatched){
  if(!(LoadedRailWorld.entity(w,origin) instanceof TrainDepotBlockEntity c))return;var device=RailwayDataManager.state(w).find(RailwayDataManager.dimension(w),origin.getX(),origin.getY(),origin.getZ());if(device==null)return;
  captureDepot(w,device,c,true);var root=data(w.getServer());var old=root.depots.get(pos(w,origin).key());var line=RailwayDataManager.state(w).line(c.selectedLineId());if(line==null)return;
  var sources=new ArrayList<Pos>();for(var g:RailPowerService.sourcesFor(w,origin.above())){sources.add(pos(w,g));if(LoadedRailWorld.entity(w,g) instanceof PowerGeneratorBlockEntity generator)captureGenerator(w,g,generator,true);}
  var plan=new Plan(path,loop,lineStart,sources,signature(line));var changed=new Depot(old.pos(),old.id(),old.config(),old.stores(),Optional.of(plan),dispatched?w.getGameTime():old.lastDispatch(),old.result(),old.overflow());root.put(changed);index(w.getServer(),changed);
 }
 private static void index(MinecraftServer s,Depot d){if(d.plan().isEmpty())return;var index=RAIL_INDEX.computeIfAbsent(s,k->new HashMap<>());for(var p:d.plan().get().path())index.computeIfAbsent(new Pos(d.pos().dimension(),p.x(),p.y(),p.z()).key(),k->new HashSet<>()).add(d.pos().key());}
 public static void begin(MinecraftServer s){STOPPING.remove(s);var root=data(s);SavedRiderProtection.protect(root,SavedRiderProtection.savedTrains(s));for(var d:root.depots.values())index(s,d);for(var t:new ArrayList<>(root.trains.values()))if(!t.occupied())root.put(new Owned(t.id(),t.origin(),t.state(),true,List.of(),departure(s.overworld(),t)));for(var w:s.getAllLevels())for(var e:new ArrayList<Entity>(all(w)))if(e instanceof LinkedTrainCar)discardIfStale(w,e);}
 private static List<Entity> all(ServerLevel w){var list=new ArrayList<Entity>();w.getAllEntities().forEach(list::add);return list;}
 public static boolean owns(MinecraftServer s,UUID id){var root=data(s);return root.trains.containsKey(id)||root.retired.contains(id);}
 public static void live(ServerLevel w,long origin,OfflineTrainState state,List<Entity> entities){
  saveTrain(w,new Owned(state.trainId(),origin,state,false,entities.stream().map(Entity::getUUID).toList(),entities.stream().anyMatch(Entity::isVehicle)));
 }
 public static void virtual(ServerLevel w,long origin,OfflineTrainState state){
  saveTrain(w,new Owned(state.trainId(),origin,state,true,List.of()));
 }
 private static void saveTrain(ServerLevel w,Owned train){
  var root=data(w.getServer());var previous=root.trains.get(train.id());
  root.put(new Owned(train.id(),train.origin(),train.state(),train.virtual(),train.members(),
          departure(w,previous==null?train:previous),train.occupied()));
 }
 private static Optional<TrainDeparture> departure(ServerLevel w,Owned t){if(t.departure().isPresent())return t.departure();var d=depot(w,t.origin());return d==null?Optional.empty():d.plan().filter(p->p.path().equals(t.state().path())).map(p->TrainDeparture.from(p.signature(),p.generators()));}
 public static Boolean departurePower(ServerLevel w,UUID id,boolean physical){return departurePower(w,id);}
 private static final Map<MinecraftServer,Map<UUID,Plan>> TRAIN_POWER_PLANS=new WeakHashMap<>();
 public static RailwayLine trainLine(ServerLevel w,UUID id,RailwayLine current){
  if(current==null)return null;var train=data(w.getServer()).trains.get(id);var route=train==null?null:train.departure().orElse(null);
  return route==null?current:route.operatingLine(current);
 }
 public static Boolean departurePower(ServerLevel w,UUID id){
  var root=data(w.getServer());var train=root.trains.get(id);if(train==null||train.state().path().isEmpty())return null;
  var route=train.departure().orElse(null);if(route==null)return null;
  var first=train.state().path().get(0);var start=new BlockPos(first.x(),first.y(),first.z());
  var currentPoint=train.state().path().get(train.state().pathIndex());var current=new BlockPos(currentPoint.x(),currentPoint.y(),currentPoint.z());
  var sources=new LinkedHashSet<Pos>(route.generators());for(var rail:List.of(start,current))if(RailPowerService.rawPowered(w,rail))for(var p:RailPowerService.sourcesFor(w,rail))if(LoadedRailWorld.entity(w,p) instanceof PowerGeneratorBlockEntity actual&&actual.active())sources.add(pos(w,p));
  if(sources.isEmpty())return false;
  if(!route.generators().equals(List.copyOf(sources))){route=route.withGenerators(List.copyOf(sources));root.put(new Owned(train.id(),train.origin(),train.state(),train.virtual(),train.members(),train.occupied(),Optional.of(route)));}
  var origin=depot(w,train.origin());if(origin!=null&&origin.plan().isPresent()&&!origin.plan().get().path().isEmpty()&&origin.plan().get().path().get(0).equals(first)){
   var savedPlan=origin.plan().get();var known=new LinkedHashSet<Pos>(savedPlan.generators());
   for(var p:RailPowerService.sourcesFor(w,start))if(LoadedRailWorld.entity(w,p) instanceof PowerGeneratorBlockEntity generator&&generator.active())known.add(pos(w,p));
   if(!savedPlan.generators().equals(List.copyOf(known)))root.put(new Depot(origin.pos(),origin.id(),origin.config(),origin.stores(),Optional.of(new Plan(savedPlan.path(),savedPlan.loop(),savedPlan.lineStart(),List.copyOf(known),savedPlan.signature())),origin.lastDispatch(),origin.result(),origin.overflow()));
  }
  var plans=TRAIN_POWER_PLANS.computeIfAbsent(w.getServer(),server->new HashMap<>());var plan=plans.get(id);
  if(plan==null||!plan.generators().equals(route.generators())){plan=new Plan(train.state().path(),train.state().loop(),train.state().loopStartIndex(),route.generators(),route.signature());plans.put(id,plan);}
  boolean sourcePowered=false;
  for(var source:route.generators()){
   Boolean feed=cn.quit5700.fabrictrain.power.PowerFeedProof.along(w,source,train.state().path(),w.hasChunkAt(current)?current:start);if(Boolean.FALSE.equals(feed))continue;
   if(LoadedRailWorld.entity(w,source.block()) instanceof PowerGeneratorBlockEntity actual){if(actual.active()&&(Boolean.TRUE.equals(feed)||!w.hasChunkAt(start)||RailPowerService.rawPowered(w,start)))sourcePowered=true;}
   else if(!w.hasChunkAt(source.block())){var saved=root.generators.get(source.key());if(saved!=null&&saved.until()>w.getGameTime())sourcePowered=true;}
  }
  return sourcePowered&&cn.quit5700.fabrictrain.power.SavedPowerPlanBridge.allowsTrain(w,"train:"+id,plan);
 }
 public static boolean discardIfStale(ServerLevel w,Entity e){if(!(e instanceof LinkedTrainCar car))return false;var root=data(w.getServer());var id=car.trainLink().trainId();var t=root.trains.get(id);if(!root.retired.contains(id)&&(t==null||!t.virtual()&&t.members().contains(e.getUUID())))return false;TrainControllerManager.silentDiscard(e,id);return true;}
 public static boolean powered(ServerLevel w,Depot d){
  if(d==null||d.plan().isEmpty())return false;
  var root=data(w.getServer());var first=d.plan().get().path().get(0);var rail=new BlockPos(first.x(),first.y(),first.z());
  for(var source:d.plan().get().generators()){
   if(Boolean.FALSE.equals(cn.quit5700.fabrictrain.power.PowerFeedProof.connected(w,source,rail)))continue;
   var g=root.generators.get(source.key());if(g==null||g.until()<=w.getGameTime())continue;
   if(LoadedRailWorld.entity(w,source.block()) instanceof PowerGeneratorBlockEntity actual){if(!actual.active())continue;if(w.hasChunkAt(rail)&&!RailPowerService.rawPowered(w,rail))continue;}
   return true;
  }return false;
 }
 public static boolean powerAt(ServerLevel w,BlockPos rail){var index=RAIL_INDEX.get(w.getServer());if(index==null)return false;var candidates=index.get(pos(w,rail).key());if(candidates==null)return false;for(String key:candidates){var d=data(w.getServer()).depots.get(key);if(d!=null&&powered(w,d))return true;}return false;}
 public static Boolean remotePowerAt(ServerLevel w,BlockPos rail){
  var index=RAIL_INDEX.get(w.getServer());if(index==null)return null;var candidates=index.get(pos(w,rail).key());if(candidates==null)return null;
  boolean known=false,power=false,deferPhysical=false;
  for(var key:candidates){var d=data(w.getServer()).depots.get(key);if(d==null||d.plan().isEmpty()||d.plan().get().generators().isEmpty())continue;
   var plan=d.plan().get();
   boolean loadedSource=plan.generators().stream().anyMatch(p->LoadedRailWorld.entity(w,p.block()) instanceof PowerGeneratorBlockEntity);
   if(loadedSource&&!cn.quit5700.fabrictrain.power.SavedPowerPlanBridge.allows(w,key,plan)){deferPhysical=true;continue;}
   known=true;power|=powered(w,d);
  }return power?Boolean.TRUE:deferPhysical?null:known?Boolean.FALSE:null;
 }
 public static Boolean linePower(ServerLevel w,long line){for(var d:data(w.getServer()).depots.values())if(d.pos().dimension().equals(RailwayDataManager.dimension(w))&&d.config().line()==line&&d.plan().isPresent())return powered(w,d);return null;}
 public static boolean due(ServerLevel w,long depot){var d=depot(w,depot);return d==null||w.getGameTime()-d.lastDispatch()>=Math.max(1,d.config().interval())*20L;}
 private static final class Mirror implements Container{
  final ServerLevel w;final Stock saved;final ArrayList<ItemStack> values;final Container physical;
  Mirror(ServerLevel w,Stock saved,Container physical){this.w=w;this.saved=saved;this.values=items(saved);this.physical=physical;}
  public boolean canPlaceItem(int slot,ItemStack stack){
   if(physical!=null)return physical.canPlaceItem(slot,stack);
   String kind=saved.block();if(kind.equals("fabric_train_simulator:power_generator"))return stack.is(Items.REDSTONE)||stack.is(Items.REDSTONE_BLOCK);
   return kind.equals("fabric_train_simulator:train_depot")||kind.equals("minecraft:chest")||kind.equals("minecraft:trapped_chest")||kind.equals("minecraft:barrel")||kind.equals("universal_chest:buffer")||kind.endsWith("shulker_box");
  }
  public int getContainerSize(){return values.size();}public boolean isEmpty(){return values.stream().allMatch(ItemStack::isEmpty);}public ItemStack getItem(int i){return values.get(i);}public ItemStack removeItem(int i,int n){var result=values.get(i).split(n);setChanged();return result;}public ItemStack removeItemNoUpdate(int i){var s=values.set(i,ItemStack.EMPTY);setChanged();return s;}public void setItem(int i,ItemStack s){values.set(i,s.copy());}public int getMaxStackSize(){return saved.limit();}public boolean stillValid(Player p){return true;}public void clearContent(){Collections.fill(values,ItemStack.EMPTY);setChanged();}
  public void setChanged(){var slots=new ArrayList<Slot>();for(int i=0;i<values.size();i++)if(!values.get(i).isEmpty())slots.add(new Slot(i,values.get(i)));data(w.getServer()).put(new Stock(saved.pos(),saved.block(),values.size(),saved.limit(),slots,true));if(physical!=null)write(physical,values);}
 }
 private static Mirror view(ServerLevel w,Pos p){
  var root=data(w.getServer());var saved=root.stocks.get(p.key());
  if(LoadedRailWorld.entity(w,p.block()) instanceof Container c){bind(w,p.block(),c);captureStock(w,p.block(),c,true);saved=root.stocks.get(p.key());return new Mirror(w,saved,c);}
  // Claim the last live inventory before detaching its delayed unload callback.
  var bound=BOUND.get(w.getServer());var previous=bound==null?null:bound.get(p.key());
  if(previous!=null){
   saved=copy(w,p.block(),previous,true);root.put(saved);
   if(previous instanceof PowerGeneratorBlockEntity g)root.put(new Generator(p,g.offlineFuelUntil(),g.offlineReservedSlot(),true));
   bound.remove(p.key(),previous);
  }
  return saved==null?null:new Mirror(w,saved,null);
 }
 private static void insert(List<? extends Container> stores,ItemStack source){
  for(var c:stores)for(int i=0;i<c.getContainerSize()&&!source.isEmpty();i++){var target=c.getItem(i);if(target.isEmpty()||!ItemStack.isSameItemSameComponents(source,target))continue;int n=Math.min(Math.min(target.getMaxStackSize(),c.getMaxStackSize())-target.getCount(),source.getCount());if(n>0){target.grow(n);source.shrink(n);c.setItem(i,target);}}
  for(var c:stores)for(int i=0;i<c.getContainerSize()&&!source.isEmpty();i++)if(c.getItem(i).isEmpty()&&c.canPlaceItem(i,source)){int n=Math.min(Math.min(source.getMaxStackSize(),c.getMaxStackSize()),source.getCount());c.setItem(i,source.copyWithCount(n));source.shrink(n);}
 }
 private static boolean take(List<? extends Container> stores,Item item,int n,boolean commit){int available=0;for(var c:stores)for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))available+=c.getItem(i).getCount();if(available<n)return false;if(commit)for(var c:stores)for(int i=0;i<c.getContainerSize()&&n>0;i++)if(c.getItem(i).is(item)){var s=c.getItem(i);int k=Math.min(s.getCount(),n);s.shrink(k);n-=k;c.setItem(i,s);}return true;}
 public static boolean dispatch(ServerLevel w,RailwayLine line){
  var d=depot(w,line.depotId());if(d==null||d.plan().isEmpty()||!d.config().enabled()||d.config().line()!=line.id()||line.status()!=LineStatus.RUNNING||d.config().maximum()<=0||!due(w,d.id()))return false;
  var registry=RailwayDataManager.state(w);if(registry.activeAutomaticTrainCount(line.id())>=d.config().maximum()||!signature(line).equals(d.plan().get().signature()))return false;
  var root=data(w.getServer());var cfg=d.config();boolean creative=cn.quit5700.fabrictrain.depot.DispatchAuthorityService.isCreative(w,d.id());var template=DepotTemplate.decode(cfg.template());if(template.cars().isEmpty())return false;
  for(var t:root.trains.values())if(t.virtual()&&t.state().lineId()==line.id()&&t.state().pathIndex()<template.cars().size()*2+4)return false;
  var stores=new ArrayList<Mirror>();for(var p:d.stores()){var v=view(w,p);if(v!=null)stores.add(v);}if(stores.isEmpty())return false;
  int passengers=(int)template.cars().stream().filter(t->t==CarType.PASSENGER).count(),cargo=template.cars().size()-passengers;
  if(!creative&&(!take(stores,ModItems.LOCOMOTIVE,1,false)||!take(stores,ModItems.PASSENGER_CAR,passengers,false)||!take(stores,ModItems.CARGO_CAR,cargo,false)))return false;
  var id=UUID.randomUUID();var plan=d.plan().get();var klass=TrainClass.valueOf(cfg.trainClass());double speed=SpeedRules.blocksPerTick(registry.classSpeedLimit(klass));
  var cars = new ArrayList<OfflineCarState>();
  boolean configuredCargo = TrainClass.CARGO.name().equals(cfg.trainClass());
  for (int index = 0; index < template.cars().size(); index++) {
   CarType type = template.cars().get(index);
   cars.add(new OfflineCarState(type, configuredCargo && type == CarType.CARGO ? template.destination(index + 1) : 0L, List.of()));
  }var state=new OfflineTrainState(id,line.id(),klass,cfg.speed(),0,0,0D,plan.loop(),plan.lineStart(),cn.quit5700.fabrictrain.freight.FreightYardService.startsAtYard(registry,line) ? 0 : 1,
          0,configuredCargo ? template.destination(0) : 0L,0,w.getGameTime(),plan.path(),cars);
  if(!creative){take(stores,ModItems.LOCOMOTIVE,1,true);take(stores,ModItems.PASSENGER_CAR,passengers,true);take(stores,ModItems.CARGO_CAR,cargo,true);stores.forEach(Mirror::setChanged);}
  virtual(w,d.id(),state);registry.ensureTrain(id,true,line.id(),ControlMode.ROUTE_AUTOMATIC);registry.registerActiveTrain(id,line.id());registry.updateTrainPresence(id,TrainPresence.OFFLINE);
  var members=new ArrayList<TrainMember>();members.add(new TrainMember(UUID.randomUUID(),TrainVehicleKind.LOCOMOTIVE,1));for(var c:cars)members.add(new TrainMember(UUID.randomUUID(),c.type()==CarType.CARGO?TrainVehicleKind.CARGO:TrainVehicleKind.PASSENGER,members.size()+1));registry.replaceTrainManifest(id,d.id(),members);
  root.put(new Depot(d.pos(),d.id(),cfg,d.stores(),d.plan(),w.getGameTime(),DepotDispatchResult.SUCCESS.ordinal(),d.overflow()));return true;
 }
 public static boolean recover(ServerLevel w,OfflineTrainState state){
  var root=data(w.getServer());var registry=RailwayDataManager.state(w);
  if(root.retired.contains(state.trainId())){registry.markTrainForDeletion(state.trainId());return true;}
  var operating=trainLine(w,state.trainId(),registry.line(state.lineId()));
  var target=operating==null?null:operating.nodeIds().stream().map(registry::device).filter(Objects::nonNull)
          .filter(device->device.type()==DeviceType.DEPOT).findFirst().orElse(null);
  if(!TrainArrivalRules.canRecover(state,operating,target)
     ||!target.dimension().equals(RailwayDataManager.dimension(w)))return false;
  var list=new ArrayList<ItemStack>();list.add(new ItemStack(ModItems.LOCOMOTIVE));for(var c:state.cars()){list.add(new ItemStack(c.type()==CarType.CARGO?ModItems.CARGO_CAR:ModItems.PASSENGER_CAR));for(var item:c.items())if(!item.isEmpty())list.add(item.copy());}
  root.recoveries.put(state.trainId(),new Recovery(state.trainId(),new Pos(target.dimension(),target.x(),target.y(),target.z()),list));root.trains.remove(state.trainId());root.retired.add(state.trainId());registry.markTrainForDeletion(state.trainId());root.setDirty();return true;
 }
 public static void deposit(ServerLevel w,UUID train,int car,long station,List<ItemStack> stacks){if(stacks.stream().allMatch(ItemStack::isEmpty))return;var id=UUID.nameUUIDFromBytes((train+":"+car+":"+station+":"+w.getGameTime()).getBytes(java.nio.charset.StandardCharsets.UTF_8));var delivery=new PendingDelivery(id,UUID.nameUUIDFromBytes((train+":"+car).getBytes(java.nio.charset.StandardCharsets.UTF_8)),station,w.getGameTime(),stacks);var root=data(w.getServer());root.deliveries.putIfAbsent(id,delivery);root.setDirty();}
 public static void captureStationStock(ServerLevel w, BlockPos station) {
  for (Direction side : Direction.values()) {
   BlockPos p = station.relative(side);
   if (LoadedRailWorld.entity(w, p) instanceof Container container) {
    var root = data(w.getServer());
    var old = root.stocks.get(pos(w,p).key());
    if (old == null || BOUND.getOrDefault(w.getServer(), Map.of()).get(pos(w,p).key()) != container)
     captureStock(w,p,container,false);
   } else if (w.hasChunkAt(p)) {
    var root = data(w.getServer());
    if (root.stocks.remove(pos(w,p).key()) != null) root.setDirty();
   }
  }
 }

 /** Keep capacity-rejected items on the car. The accepted part uses the existing persisted delivery ledger. */
 public static List<ItemStack> unloadVirtual(ServerLevel w, UUID train, int car, long stationId, List<ItemStack> stacks) {
  RailwayDevice station = RailwayDataManager.state(w).device(stationId);
  if (station == null || stacks.isEmpty()) return stacks;
  BlockPos stationPos = new BlockPos(station.x(), station.y(), station.z());
  boolean loaded = w.hasChunkAt(stationPos);
  for (Direction side : Direction.values()) loaded &= w.hasChunkAt(stationPos.relative(side));
  if (loaded) return deliver(w, stationPos, stacks);

  var root = data(w.getServer());
  List<Mirror> capacity = new ArrayList<>();
  for (Direction side : Direction.values()) {
   var p = pos(w, stationPos.relative(side));
   if (w.hasChunkAt(p.block())) {
    if (LoadedRailWorld.entity(w, p.block()) instanceof Container container)
     capacity.add(new Mirror(w, copy(w, p.block(), container, false), null));
   } else {
    Stock snapshot = root.stocks.get(p.key());
    if (snapshot != null) capacity.add(new Mirror(w, snapshot, null));
   }
  }
  if (capacity.isEmpty()) return stacks;
  for (PendingDelivery pending : CargoLedgerService.state(w).deliveries()) {
   if (pending.stationId() == stationId)
    for (ItemStack stack : pending.stacks()) insert(capacity, stack.copy());
  }
  List<ItemStack> accepted = new ArrayList<>(), remainder = new ArrayList<>();
  for (ItemStack original : stacks) {
   ItemStack left = original.copy();
   insert(capacity, left);
   int moved = original.getCount() - left.getCount();
   if (moved > 0) accepted.add(original.copyWithCount(moved));
   if (!left.isEmpty()) remainder.add(left);
  }
  if (owns(w.getServer(), train)) deposit(w, train, car, stationId, accepted);
  else CargoLedgerService.depositArrived(w, UUID.nameUUIDFromBytes(
      (train + ":" + car).getBytes(java.nio.charset.StandardCharsets.UTF_8)), stationId, accepted);
  return remainder;
 }

 public static int unloadLoaded(ServerLevel w,BlockPos station,CargoCarEntity cargo){
  var source=new ArrayList<ItemStack>();int before=0;for(int i=0;i<cargo.getContainerSize();i++){var s=cargo.getItem(i);if(!s.isEmpty()){source.add(s.copy());before+=s.getCount();}}
  var remaining=deliver(w,station,source);int after=remaining.stream().mapToInt(ItemStack::getCount).sum();if(before==after)return 0;
  cargo.clearContent();for(int i=0;i<remaining.size();i++)cargo.setItem(i,remaining.get(i).copy());cargo.setChanged();return before-after;
 }
 public static List<ItemStack> deliver(ServerLevel w,BlockPos station,List<ItemStack> stacks){var destinations=new ArrayList<Mirror>();for(var direction:Direction.values()){var p=station.relative(direction);if(LoadedRailWorld.entity(w,p) instanceof Container){var v=view(w,pos(w,p));if(v!=null)destinations.add(v);}}if(destinations.isEmpty())return stacks.stream().map(ItemStack::copy).toList();var remaining=new ArrayList<ItemStack>();for(var s:stacks){var copy=s.copy();insert(destinations,copy);if(!copy.isEmpty())remaining.add(copy);}destinations.forEach(Mirror::setChanged);return remaining;}
 private static void settleFuel(ServerLevel w,Generator g){
  if(LoadedRailWorld.entity(w,g.pos().block()) instanceof PowerGeneratorBlockEntity)return;var c=view(w,g.pos());if(c==null)return;g=data(w.getServer()).generators.getOrDefault(g.pos().key(),g);long until=g.until();int reserved=g.reserved();int budget=c.getContainerSize()*64+1;
  while(until<=w.getGameTime()&&budget-->0){if(reserved>=0&&reserved<c.getContainerSize()){var old=c.getItem(reserved);if(!old.isEmpty())old.shrink(1);c.setItem(reserved,old);}reserved=-1;long duration=0;for(int i=0;i<c.getContainerSize();i++){var s=c.getItem(i);if(s.is(Items.REDSTONE_BLOCK))duration=PowerGeneratorBlockEntity.REDSTONE_BLOCK_TICKS;else if(s.is(Items.REDSTONE))duration=PowerGeneratorBlockEntity.REDSTONE_DUST_TICKS;if(duration>0){reserved=i;break;}}if(reserved<0){until=0;break;}until=(until<=0?w.getGameTime():until)+duration;}
  if(until!=g.until()||reserved!=g.reserved()){c.setChanged();data(w.getServer()).put(new Generator(g.pos(),until,reserved,true));}
 }
 private static void returns(ServerLevel w){var root=data(w.getServer());for(var r:new ArrayList<>(root.recoveries.values())){if(!r.target().dimension().equals(RailwayDataManager.dimension(w)))continue;var d=root.depots.get(r.target().key());if(d==null)continue;var stores=new ArrayList<Mirror>();for(var p:d.stores()){var v=view(w,p);if(v!=null)stores.add(v);}if(stores.isEmpty())continue;var overflow=new ArrayList<ItemStack>(d.overflow());for(var item:r.items()){var copy=item.copy();insert(stores,copy);if(!copy.isEmpty())overflow.add(copy);}stores.forEach(Mirror::setChanged);root.put(new Depot(d.pos(),d.id(),d.config(),d.stores(),d.plan(),d.lastDispatch(),d.result(),overflow));root.recoveries.remove(r.train());root.setDirty();}}
 public static void finish(MinecraftServer s){
  var root=data(s);for(var w:s.getAllLevels()){
   var registry=RailwayDataManager.state(w);for(var device:registry.devices().values())if(device.type()==DeviceType.DEPOT&&device.dimension().equals(RailwayDataManager.dimension(w))&&LoadedRailWorld.entity(w,new BlockPos(device.x(),device.y(),device.z())) instanceof TrainDepotBlockEntity depot)captureDepot(w,device,depot,false);
   var positions=new HashSet<BlockPos>(RailPowerService.generatorPositions(w));for(var g:root.generators.values())if(g.pos().dimension().equals(RailwayDataManager.dimension(w)))positions.add(g.pos().block());for(var p:positions)if(LoadedRailWorld.entity(w,p) instanceof PowerGeneratorBlockEntity g)captureGenerator(w,p,g,false);
   for(var stock:new ArrayList<>(root.stocks.values()))if(stock.pos().dimension().equals(RailwayDataManager.dimension(w))&&LoadedRailWorld.entity(w,stock.pos().block()) instanceof Container c)captureStock(w,stock.pos().block(),c,stock.owned());
   if(s.getTickCount()%20==0){for(var g:new ArrayList<>(root.generators.values()))if(g.pos().dimension().equals(RailwayDataManager.dimension(w)))settleFuel(w,g);returns(w);}
   for(var d:new ArrayList<>(root.depots.values()))if(d.pos().dimension().equals(RailwayDataManager.dimension(w))&&!d.overflow().isEmpty()&&LoadedRailWorld.entity(w,d.pos().block()) instanceof TrainDepotBlockEntity){boolean ok=true;var added=new ArrayList<net.minecraft.world.entity.item.ItemEntity>();for(var stack:d.overflow()){var e=new net.minecraft.world.entity.item.ItemEntity(w,d.pos().x()+.5,d.pos().y()+1.1,d.pos().z()+.5,stack.copy());e.setDefaultPickUpDelay();if(!w.addFreshEntity(e)){ok=false;break;}added.add(e);}if(ok)root.put(new Depot(d.pos(),d.id(),d.config(),d.stores(),d.plan(),d.lastDispatch(),d.result(),List.of()));else added.forEach(Entity::discard);}
  }
 }
}
