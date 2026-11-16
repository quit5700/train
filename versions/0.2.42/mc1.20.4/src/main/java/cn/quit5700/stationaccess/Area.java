package cn.quit5700.stationaccess;
import java.util.*;
import cn.quit5700.stationaccess.core.Box;
import net.minecraft.core.BlockPos;
public final class Area {
 public String id=UUID.randomUUID().toString(),owner,dimension;public Box box;public Set<Long> holes=new HashSet<>(),chunks=new HashSet<>();
 public Area(String owner,String dimension,Box box){this.owner=owner;this.dimension=dimension;this.box=box;}
 public Box.Cell first,second;public Boolean shown;public int color=3;
 public boolean visible(){return shown==null||shown;}
 public Box.Cell anchor(){return first==null?new Box.Cell(box.x0(),box.y0(),box.z0()):first;}
 public Box.Cell end(){return second==null?new Box.Cell(box.x1(),box.y1(),box.z1()):second;}
 public boolean covers(BlockPos p){return box.shell(new Box.Cell(p.getX(),p.getY(),p.getZ()))&&!holes.contains(p.asLong());}
 public boolean closed(Box.Cell p){return !holes.contains(BlockPos.asLong(p.x(),p.y(),p.z()));}
}
