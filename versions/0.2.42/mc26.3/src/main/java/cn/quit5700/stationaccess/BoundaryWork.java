package cn.quit5700.stationaccess;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
/** Legacy-only removal. New access areas never write blocks into the world. */
public final class BoundaryWork {
 private record Key(LevelChunk chunk,int section){}
 private static final class Job {final ServerLevel level;final LevelChunk chunk;final int section,minY;int cursor;Job(ServerLevel level,LevelChunk chunk,int section){this.level=level;this.chunk=chunk;this.section=section;this.minY=level.getMinY()+section*16;}}
 private static final Set<LevelChunk> LOADED=Collections.newSetFromMap(new IdentityHashMap<>());
 private static final Set<Key> QUEUED=new HashSet<>();private static final Deque<Job> JOBS=new ArrayDeque<>();
 public static void register(){ServerChunkEvents.CHUNK_LOAD.register((world,chunk,newlyGenerated)->{schedule(world,chunk);});ServerChunkEvents.CHUNK_UNLOAD.register((world,chunk)->LOADED.remove(chunk));}
 static void schedule(ServerLevel level,LevelChunk chunk){LOADED.add(chunk);var sections=chunk.getSections();for(int i=0;i<sections.length;i++)if(sections[i].maybeHas(s->s.is(StationAccess.BOUNDARY))&&QUEUED.add(new Key(chunk,i)))JOBS.addLast(new Job(level,chunk,i));}
 public static void refresh(){/* No materialisation or volume-sized work is scheduled. */}
 public static int tick(MinecraftServer server,AccessData data){
  if(data.state.logicalBoundaryVersion<2){for(var area:data.state.areas)if(area.chunks!=null)area.chunks.clear();data.state.retired.clear();data.state.logicalBoundaryVersion=2;data.setDirty();}
  int work=0,popped=0;while(work<128&&!JOBS.isEmpty()&&popped++<256){Job j=JOBS.removeFirst();Key key=new Key(j.chunk,j.section);if(!LOADED.contains(j.chunk)){QUEUED.remove(key);continue;}
   int n=j.cursor++;work++;var section=j.chunk.getSections()[j.section];if(section.getBlockState(n&15,n>>8,n>>4&15).is(StationAccess.BOUNDARY)){var p=new BlockPos(j.chunk.getPos().getMinBlockX()+(n&15),j.minY+(n>>8),j.chunk.getPos().getMinBlockZ()+(n>>4&15));if(j.level.getBlockState(p).is(StationAccess.BOUNDARY)){j.level.setBlock(p,Blocks.AIR.defaultBlockState(),3);j.level.getChunkSource().getLightEngine().checkBlock(p);}}
   if(j.cursor<4096)JOBS.addLast(j);else QUEUED.remove(key);
  }return work;
 }
 public static void clear(){LOADED.clear();QUEUED.clear();JOBS.clear();}
}
