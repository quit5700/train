package cn.quit5700.fabrictrain.power;

import cn.quit5700.fabrictrain.offline.InfrastructureData;
import cn.quit5700.fabrictrain.offline.InfrastructureService;
import cn.quit5700.fabrictrain.registry.ModBlocks;
import cn.quit5700.fabrictrain.route.TrackPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import java.util.*;

/** Electrical conductors are independent of a timetable or a dispatched train's route. */
public final class PowerFeedProof {
 private record Cached(InfrastructureData.Feed feed,LoadedRouteProof proof){}
 private static final Map<Object,Map<String,Cached>> CACHE=new WeakHashMap<>();
 private static final Map<Object,LoadedRouteProof.Budget> BUDGETS=new WeakHashMap<>();
 private static final Map<Object,Integer> TICKS=new WeakHashMap<>();
 private static final Map<Object,Map<String,Search>> SEARCHES=new WeakHashMap<>();
 private static final class Search{final Map<BlockPos,BlockPos> parents=new HashMap<>();final ArrayDeque<BlockPos> queue=new ArrayDeque<>();final long epoch;Search(long epoch){this.epoch=epoch;}}
 private static LoadedRouteProof.Budget budget(ServerLevel w){if(TICKS.getOrDefault(w.getServer(),-1)!=w.getServer().getTickCount()){TICKS.put(w.getServer(),w.getServer().getTickCount());BUDGETS.put(w.getServer(),new LoadedRouteProof.Budget(2048));}return BUDGETS.get(w.getServer());}
 public static void clear(){CACHE.clear();BUDGETS.clear();TICKS.clear();SEARCHES.clear();}
 private PowerFeedProof(){}
 public static Boolean along(ServerLevel w,InfrastructureData.Pos source,List<TrackPoint> route,BlockPos anchor){
  boolean blocked=false;for(var feed:InfrastructureService.data(w.getServer()).feeds.values()){
   if(!feed.key().startsWith(source.key()+"->")||feed.path().isEmpty())continue;var target=feed.path().get(feed.path().size()-1);
   if(!route.contains(new TrackPoint(target.x(),target.y(),target.z())))continue;
   Boolean valid=connected(w,source,target.block());if(Boolean.TRUE.equals(valid))return true;blocked|=Boolean.FALSE.equals(valid);
  }
  return blocked?Boolean.FALSE:connected(w,source,anchor);
 }
 public static Boolean connected(ServerLevel w,InfrastructureData.Pos source,BlockPos start){
  var root=InfrastructureService.data(w.getServer());String key=source.key()+"->"+InfrastructureService.pos(w,start).key();var feed=root.feeds.get(key);
  if(feed==null){feed=discover(w,key,source.block(),start);if(feed==null)return null;root.feeds.put(key,feed);root.setDirty();}
  boolean blocked=false;
  for(var fault:new ArrayList<>(feed.faults())){
   var chunk=w.getChunkSource().getChunkNow(fault.x()>>4,fault.z()>>4);
   if(chunk==null){blocked=true;continue;}
   if(matches(chunk.getBlockState(fault.block()),feed.wires().contains(fault))){feed.faults().remove(fault);root.setDirty();}else blocked=true;
  }
  if(blocked)return false;
  var entries=CACHE.computeIfAbsent(w.getServer(),s->new HashMap<>());var entry=entries.get(key);
  if(entry==null||entry.feed()!=feed){entry=new Cached(feed,new LoadedRouteProof(feed.path().stream().map(p->new TrackPoint(p.x(),p.y(),p.z())).toList()));entries.put(key,entry);}
  final var observed=feed;
  var result=entry.proof().inspect(new LoadedRouteProof.View(){
   public Object chunk(int x,int z){return w.getChunkSource().getChunkNow(x,z);}
   public boolean signal(Object c,BlockPos p){var point=InfrastructureService.pos(w,p);boolean intact=matches(((LevelChunk)c).getBlockState(p),observed.wires().contains(point));if(!intact&&observed.faults().add(point))root.setDirty();return intact;}
  },SavedPowerPlanBridge.epoch(w),budget(w));
  return result.complete()&&result.intact();
 }
 private static boolean matches(BlockState state,boolean wire){return wire?state.is(Blocks.REDSTONE_WIRE):rail(state);}
 private static boolean rail(BlockState state){return state.is(ModBlocks.SIGNAL_RAIL)||state.is(ModBlocks.SWITCH_RAIL);}
 private static BlockState state(ServerLevel w,BlockPos p){var c=w.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4);return c==null?null:c.getBlockState(p);}
 private static InfrastructureData.Feed discover(ServerLevel w,String key,BlockPos source,BlockPos target){
  if(state(w,source)==null||state(w,target)==null)return null;
  var searches=SEARCHES.computeIfAbsent(w.getServer(),server->new HashMap<>());long epoch=SavedPowerPlanBridge.epoch(w);var search=searches.get(key);
  if(search==null||search.epoch!=epoch){search=new Search(epoch);searches.put(key,search);
   for(var direction:Direction.values()){var p=source.relative(direction);var state=state(w,p);if(state!=null&&state.is(Blocks.REDSTONE_WIRE)){search.parents.put(p,source);search.queue.add(p);}}
  }
  var parents=search.parents;var queue=search.queue;var remaining=budget(w);
  while(!queue.isEmpty()&&parents.size()<=120000&&remaining.take()){var p=queue.removeFirst();var current=state(w,p);if(current==null)continue;
   if(p.equals(target)){
    var path=new ArrayList<InfrastructureData.Pos>();var wires=new HashSet<InfrastructureData.Pos>();
    for(var cursor=p;!cursor.equals(source);cursor=parents.get(cursor)){var pos=InfrastructureService.pos(w,cursor);path.add(pos);if(state(w,cursor).is(Blocks.REDSTONE_WIRE))wires.add(pos);}
    Collections.reverse(path);searches.remove(key);return new InfrastructureData.Feed(key,path,wires,new HashSet<>());
   }
   if(current.is(Blocks.REDSTONE_WIRE)){
    for(var direction:Direction.values()){var n=p.relative(direction);var s=state(w,n);if(s!=null&&(s.is(Blocks.REDSTONE_WIRE)||rail(s))&&!parents.containsKey(n)){parents.put(n,p);queue.add(n);}}
   }else if(rail(current)){
    for(var direction:Direction.Plane.HORIZONTAL)for(int dy=-1;dy<=1;dy++){var n=p.relative(direction).offset(0,dy,0);var s=state(w,n);if(s!=null&&rail(s)&&!parents.containsKey(n)){parents.put(n,p);queue.add(n);}}
   }
  }return null;
 }
}
