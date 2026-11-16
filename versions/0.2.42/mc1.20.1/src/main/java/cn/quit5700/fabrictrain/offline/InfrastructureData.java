package cn.quit5700.fabrictrain.offline;

import cn.quit5700.fabrictrain.FabricTrainMod;
import cn.quit5700.fabrictrain.freight.FreightYard;
import cn.quit5700.fabrictrain.cargo.PendingDelivery;
import cn.quit5700.fabrictrain.route.TrackPoint;
import cn.quit5700.fabrictrain.train.OfflineTrainState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.datafix.DataFixTypes;
import java.util.*;

/** One saved-data transaction domain for owned trains, vehicle stock, fuel and pending returns. */
public final class InfrastructureData extends SavedData {
 public static final Codec<UUID> UUID_CODEC=Codec.STRING.xmap(UUID::fromString,UUID::toString);
 public record DispatchAuthority(UUID player,boolean creative){public static final Codec<DispatchAuthority> CODEC=RecordCodecBuilder.create(i->i.group(UUID_CODEC.fieldOf("player").forGetter(DispatchAuthority::player),Codec.BOOL.fieldOf("creative").forGetter(DispatchAuthority::creative)).apply(i,DispatchAuthority::new));}
 public record Pos(String dimension,int x,int y,int z){
  public static final Codec<Pos> CODEC=RecordCodecBuilder.create(i->i.group(Codec.STRING.fieldOf("dimension").forGetter(Pos::dimension),Codec.INT.fieldOf("x").forGetter(Pos::x),Codec.INT.fieldOf("y").forGetter(Pos::y),Codec.INT.fieldOf("z").forGetter(Pos::z)).apply(i,Pos::new));
  public BlockPos block(){return new BlockPos(x,y,z);} public String key(){return dimension+"/"+x+","+y+","+z;}
 }
 public record Slot(int slot,ItemStack stack){
  public static final Codec<Slot> CODEC=RecordCodecBuilder.create(i->i.group(Codec.INT.fieldOf("slot").forGetter(Slot::slot),cn.quit5700.fabrictrain.compat.OptionalItemStackCodec.CODEC.fieldOf("stack").forGetter(Slot::stack)).apply(i,Slot::new));
  public Slot{stack=stack.copy();}
 }
 public record Stock(Pos pos,String block,int size,int limit,List<Slot> slots,boolean owned){
  public static final Codec<Stock> CODEC=RecordCodecBuilder.create(i->i.group(Pos.CODEC.fieldOf("position").forGetter(Stock::pos),Codec.STRING.fieldOf("block").forGetter(Stock::block),Codec.INT.fieldOf("size").forGetter(Stock::size),Codec.INT.fieldOf("limit").forGetter(Stock::limit),Slot.CODEC.listOf().fieldOf("slots").forGetter(Stock::slots),Codec.BOOL.fieldOf("owned").forGetter(Stock::owned)).apply(i,Stock::new));
  public Stock{slots=List.copyOf(slots);if(size<0||size>65536)throw new IllegalArgumentException("Invalid managed container size");}
 }
 public record Config(long line,String template,String trainClass,int interval,int maximum,boolean enabled,boolean creative,double speed){
  public static final Codec<Config> CODEC=RecordCodecBuilder.create(i->i.group(Codec.LONG.fieldOf("line").forGetter(Config::line),Codec.STRING.fieldOf("template").forGetter(Config::template),Codec.STRING.fieldOf("class").forGetter(Config::trainClass),Codec.INT.fieldOf("interval").forGetter(Config::interval),Codec.INT.fieldOf("maximum").forGetter(Config::maximum),Codec.BOOL.fieldOf("enabled").forGetter(Config::enabled),Codec.BOOL.fieldOf("creative").forGetter(Config::creative),Codec.DOUBLE.fieldOf("speed").forGetter(Config::speed)).apply(i,Config::new));
 }
 public static final Codec<TrackPoint> TRACK=RecordCodecBuilder.create(i->i.group(Codec.INT.fieldOf("x").forGetter(TrackPoint::x),Codec.INT.fieldOf("y").forGetter(TrackPoint::y),Codec.INT.fieldOf("z").forGetter(TrackPoint::z)).apply(i,TrackPoint::new));
 public record Plan(List<TrackPoint> path,boolean loop,int lineStart,List<Pos> generators,String signature){
  public static final Codec<Plan> CODEC=RecordCodecBuilder.create(i->i.group(TRACK.listOf().fieldOf("path").forGetter(Plan::path),Codec.BOOL.fieldOf("loop").forGetter(Plan::loop),Codec.INT.fieldOf("line_start").forGetter(Plan::lineStart),Pos.CODEC.listOf().fieldOf("generators").forGetter(Plan::generators),Codec.STRING.fieldOf("signature").forGetter(Plan::signature)).apply(i,Plan::new));
  public Plan{path=List.copyOf(path);generators=List.copyOf(generators);}
 }
 public record Depot(Pos pos,long id,Config config,List<Pos> stores,Optional<Plan> plan,long lastDispatch,int result,List<ItemStack> overflow){
  public static final Codec<Depot> CODEC=RecordCodecBuilder.create(i->i.group(Pos.CODEC.fieldOf("position").forGetter(Depot::pos),Codec.LONG.fieldOf("id").forGetter(Depot::id),Config.CODEC.fieldOf("config").forGetter(Depot::config),Pos.CODEC.listOf().fieldOf("stores").forGetter(Depot::stores),Plan.CODEC.optionalFieldOf("plan").forGetter(Depot::plan),Codec.LONG.fieldOf("last_dispatch").forGetter(Depot::lastDispatch),Codec.INT.fieldOf("result").forGetter(Depot::result),ItemStack.CODEC.listOf().fieldOf("overflow").forGetter(Depot::overflow)).apply(i,Depot::new));
  public Depot{stores=List.copyOf(stores);overflow=overflow.stream().map(ItemStack::copy).toList();}
 }
 public record Generator(Pos pos,long until,int reserved,boolean owned){
  public static final Codec<Generator> CODEC=RecordCodecBuilder.create(i->i.group(Pos.CODEC.fieldOf("position").forGetter(Generator::pos),Codec.LONG.fieldOf("until").forGetter(Generator::until),Codec.INT.fieldOf("reserved").forGetter(Generator::reserved),Codec.BOOL.fieldOf("owned").forGetter(Generator::owned)).apply(i,Generator::new));
 }
 public record Feed(String key,List<Pos> path,Set<Pos> wires,Set<Pos> faults){
  public static final Codec<Feed> CODEC=RecordCodecBuilder.create(i->i.group(Codec.STRING.fieldOf("key").forGetter(Feed::key),Pos.CODEC.listOf().fieldOf("path").forGetter(Feed::path),Pos.CODEC.listOf().xmap(v->(Set<Pos>)new HashSet<Pos>(v),v->List.copyOf(v)).fieldOf("wires").forGetter(Feed::wires),Pos.CODEC.listOf().xmap(v->(Set<Pos>)new HashSet<Pos>(v),v->List.copyOf(v)).optionalFieldOf("faults",Set.of()).forGetter(Feed::faults)).apply(i,Feed::new));
  public Feed{path=List.copyOf(path);wires=Set.copyOf(wires);faults=new HashSet<>(faults);}
 }
 public record Owned(UUID id,long origin,OfflineTrainState state,boolean virtual,List<UUID> members,boolean occupied,Optional<TrainDeparture> departure){
  public static final Codec<Owned> CODEC=RecordCodecBuilder.create(i->i.group(UUID_CODEC.fieldOf("id").forGetter(Owned::id),Codec.LONG.fieldOf("origin").forGetter(Owned::origin),OfflineTrainState.CODEC.fieldOf("state").forGetter(Owned::state),Codec.BOOL.fieldOf("virtual").forGetter(Owned::virtual),UUID_CODEC.listOf().fieldOf("members").forGetter(Owned::members),Codec.BOOL.optionalFieldOf("occupied",false).forGetter(Owned::occupied),TrainDeparture.CODEC.optionalFieldOf("departure").forGetter(Owned::departure)).apply(i,Owned::new));
  public Owned(UUID id,long origin,OfflineTrainState state,boolean virtual,List<UUID> members){this(id,origin,state,virtual,members,false,Optional.empty());}
  public Owned(UUID id,long origin,OfflineTrainState state,boolean virtual,List<UUID> members,boolean occupied){this(id,origin,state,virtual,members,occupied,Optional.empty());}
  public Owned(UUID id,long origin,OfflineTrainState state,boolean virtual,List<UUID> members,Optional<TrainDeparture> departure){this(id,origin,state,virtual,members,false,departure);}
  public Owned(UUID id,long origin,OfflineTrainState state,boolean virtual,List<UUID> members,Optional<TrainDeparture> departure,boolean occupied){this(id,origin,state,virtual,members,occupied,departure);}
  public Owned{members=List.copyOf(members);departure=Objects.requireNonNull(departure);}
 }
 public record Recovery(UUID train,Pos target,List<ItemStack> items){
  public static final Codec<Recovery> CODEC=RecordCodecBuilder.create(i->i.group(UUID_CODEC.fieldOf("train").forGetter(Recovery::train),Pos.CODEC.fieldOf("target").forGetter(Recovery::target),ItemStack.CODEC.listOf().fieldOf("items").forGetter(Recovery::items)).apply(i,Recovery::new));
  public Recovery{items=items.stream().filter(s->!s.isEmpty()).map(ItemStack::copy).toList();}
 }
 public static final Codec<InfrastructureData> CODEC=RecordCodecBuilder.create(i->i.group(
  Stock.CODEC.listOf().optionalFieldOf("containers",List.of()).forGetter(d->List.copyOf(d.stocks.values())),
  Depot.CODEC.listOf().optionalFieldOf("depots",List.of()).forGetter(d->List.copyOf(d.depots.values())),
  Generator.CODEC.listOf().optionalFieldOf("generators",List.of()).forGetter(d->List.copyOf(d.generators.values())),
  Owned.CODEC.listOf().optionalFieldOf("trains",List.of()).forGetter(d->List.copyOf(d.trains.values())),
  Recovery.CODEC.listOf().optionalFieldOf("recoveries",List.of()).forGetter(d->List.copyOf(d.recoveries.values())),
  UUID_CODEC.listOf().optionalFieldOf("retired",List.of()).forGetter(d->List.copyOf(d.retired)),
  PendingDelivery.CODEC.listOf().optionalFieldOf("deliveries",List.of()).forGetter(d->List.copyOf(d.deliveries.values())),
  FreightYard.CODEC.listOf().optionalFieldOf("freight_yards",List.of()).forGetter(d->List.copyOf(d.freightYards.values())),
  Codec.unboundedMap(Codec.STRING,DispatchAuthority.CODEC).optionalFieldOf("dispatch_authorities",Map.of()).forGetter(d->Map.copyOf(d.dispatchAuthorities)),
  Feed.CODEC.listOf().optionalFieldOf("power_feeds",List.of()).forGetter(d->List.copyOf(d.feeds.values())),
  Pos.CODEC.listOf().optionalFieldOf("rail_faults",List.of()).forGetter(d->List.copyOf(d.railFaults))
 ).apply(i,InfrastructureData::new));
    public static final String DATA_ID = FabricTrainMod.MOD_ID + "_offline_infrastructure";

 public final Map<String,DispatchAuthority> dispatchAuthorities=new LinkedHashMap<>();
 public final Map<Long,FreightYard> freightYards=new LinkedHashMap<>();
 public final Map<String,Stock> stocks=new LinkedHashMap<>();
 public final Map<String,Depot> depots=new LinkedHashMap<>();
 public final Map<String,Generator> generators=new LinkedHashMap<>();
 public final Map<String,Feed> feeds=new LinkedHashMap<>();
 public final Set<Pos> railFaults=new HashSet<>();
 public final Map<UUID,Owned> trains=new LinkedHashMap<>();
 public final Map<UUID,Recovery> recoveries=new LinkedHashMap<>();
 public final Set<UUID> retired=new LinkedHashSet<>();
 public final Map<UUID,PendingDelivery> deliveries=new LinkedHashMap<>();
 public InfrastructureData(){}
 private InfrastructureData(List<Stock>s,List<Depot>d,List<Generator>g,List<Owned>t,List<Recovery>r,List<UUID>ret,List<PendingDelivery>del,List<FreightYard>yards,Map<String,DispatchAuthority>auth,List<Feed>feeds,List<Pos>faults){feeds.forEach(v->this.feeds.put(v.key(),v));railFaults.addAll(faults);dispatchAuthorities.putAll(auth);yards.forEach(y->freightYards.put(y.id,y));s.forEach(v->stocks.put(v.pos().key(),v));d.forEach(v->depots.put(v.pos().key(),v));g.forEach(v->generators.put(v.pos().key(),v));t.forEach(v->trains.put(v.id(),v));r.forEach(v->recoveries.put(v.train(),v));retired.addAll(ret);del.forEach(v->deliveries.put(v.shipmentId(),v));}
 public void put(Stock s){stocks.put(s.pos().key(),s);setDirty();} public void put(Depot d){depots.put(d.pos().key(),d);setDirty();} public void put(Generator g){generators.put(g.pos().key(),g);setDirty();} public void put(Owned t){
  if(t.departure().isEmpty()){
   var previous=trains.get(t.id());var route=previous==null?Optional.<TrainDeparture>empty():previous.departure();
   if(route.isEmpty())for(var d:depots.values())if(d.id()==t.origin()&&d.config().line()==t.state().lineId()&&d.plan().isPresent()&&d.plan().get().path().equals(t.state().path())){route=Optional.ofNullable(TrainDeparture.from(d.plan().get().signature(),d.plan().get().generators()));break;}
   t=new Owned(t.id(),t.origin(),t.state(),t.virtual(),t.members(),t.occupied(),route);
  }
  trains.put(t.id(),t);setDirty();
 }
    public static InfrastructureData load(CompoundTag tag) {
        return CODEC.parse(NbtOps.INSTANCE, tag)
                .result().orElseGet(InfrastructureData::new);
    }

    @Override public CompoundTag save(CompoundTag tag) {
        return CODEC.encodeStart(NbtOps.INSTANCE, this)
                .result().filter(CompoundTag.class::isInstance).map(CompoundTag.class::cast).orElse(tag);
    }
}
