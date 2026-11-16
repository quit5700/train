package cn.quit5700.fabrictrain.power;

import cn.quit5700.fabrictrain.offline.InfrastructureData.Plan;
import cn.quit5700.fabrictrain.registry.ModBlocks;
import cn.quit5700.fabrictrain.data.RailwayDataManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import java.util.*;

/** Bounded proof for saved connectivity across unloaded route chunks. Server thread only. */
public final class SavedPowerPlanBridge {
    private static final int BLOCK_CHECKS_PER_TICK=2048;
    private static final Map<MinecraftServer, Context> CONTEXTS=new WeakHashMap<>();
    private static final class Context {
        int tick=Integer.MIN_VALUE;
        LoadedRouteProof.Budget budget=new LoadedRouteProof.Budget(0);
        final Map<String,Long> epochs=new HashMap<>();
        final Map<String,Entry> plans=new HashMap<>();
    }
    private static final class Entry {
        final Plan plan;final LoadedRouteProof proof;
        int checkedTick=Integer.MIN_VALUE;long checkedEpoch=Long.MIN_VALUE;boolean allowed;
        Entry(Plan plan,LoadedRouteProof proof){this.plan=plan;this.proof=proof;}
    }
    private SavedPowerPlanBridge() {}
    public static long epoch(ServerLevel level){return CONTEXTS.computeIfAbsent(level.getServer(),s->new Context()).epochs.getOrDefault(RailwayDataManager.dimension(level),0L);}
    public static boolean allows(ServerLevel level,String key,Plan plan){return check(level,key,plan,true);}
    public static boolean allowsTrain(ServerLevel level,String key,Plan plan){return check(level,key,plan,false);}
    private static boolean check(ServerLevel level, String key, Plan plan,boolean requireUnloaded) {
        var context=CONTEXTS.computeIfAbsent(level.getServer(),s->new Context());
        int tick=level.getServer().getTickCount();
        if(context.tick!=tick){context.tick=tick;context.budget=new LoadedRouteProof.Budget(BLOCK_CHECKS_PER_TICK);}
        var entry=context.plans.get(key);
        if(entry==null||entry.plan!=plan){entry=new Entry(plan,new LoadedRouteProof(plan.path()));context.plans.put(key,entry);}
        var root=cn.quit5700.fabrictrain.offline.InfrastructureService.data(level.getServer());
        for(var fault:new ArrayList<>(root.railFaults))if(fault.dimension().equals(RailwayDataManager.dimension(level))&&plan.path().contains(new cn.quit5700.fabrictrain.route.TrackPoint(fault.x(),fault.y(),fault.z()))){
            var chunk=level.getChunkSource().getChunkNow(fault.x()>>4,fault.z()>>4);
            if(chunk==null)return false;var state=chunk.getBlockState(fault.block());
            if(!state.is(ModBlocks.SIGNAL_RAIL)&&!state.is(ModBlocks.SWITCH_RAIL))return false;
            root.railFaults.remove(fault);root.setDirty();
        }
        long epoch=context.epochs.getOrDefault(RailwayDataManager.dimension(level),0L);
        if(entry.checkedTick==tick&&entry.checkedEpoch==epoch)return entry.allowed;
        entry.checkedTick=tick;entry.checkedEpoch=epoch;
        var inspected=entry.proof.inspect(new LoadedRouteProof.View(){
            public Object chunk(int x,int z){return level.getChunkSource().getChunkNow(x,z);}
            public boolean signal(Object chunk,net.minecraft.core.BlockPos pos){
                var state=((LevelChunk)chunk).getBlockState(pos);
                boolean intact=state.is(ModBlocks.SIGNAL_RAIL)||state.is(ModBlocks.SWITCH_RAIL);
                if(!intact&&root.railFaults.add(cn.quit5700.fabrictrain.offline.InfrastructureService.pos(level,pos)))root.setDirty();return intact;
            }
        },epoch,context.budget);
        entry.allowed=requireUnloaded?inspected.allowsBridge():inspected.complete()&&inspected.intact();
        return entry.allowed;
    }
    public static void invalidate(ServerLevel level){
        var context=CONTEXTS.get(level.getServer());
        if(context!=null)context.epochs.merge(RailwayDataManager.dimension(level),1L,Long::sum);
    }
    public static void clear(){CONTEXTS.clear();}
}
