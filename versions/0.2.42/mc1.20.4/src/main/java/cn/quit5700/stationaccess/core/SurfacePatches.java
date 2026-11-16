package cn.quit5700.stationaccess.core;

import java.util.*;

/** A large area's surface is evaluated only where it intersects a single loaded chunk. */
public final class SurfacePatches {
    public record Patch(int x,int y,int z,int w,int h,int d) {
        public long size(){return (long)w*h*d;}
        public Box.Cell at(long i){
            if(i<0||i>=size())throw new IndexOutOfBoundsException();
            return new Box.Cell(x+(int)(i%w),y+(int)((i/w)%h),z+(int)(i/((long)w*h)));
        }
    }
    public static List<Patch> forChunk(Box b,int cx,int cz){
        int minX=Math.max(b.x0(),cx*16),maxX=Math.min(b.x1(),cx*16+15);
        int minZ=Math.max(b.z0(),cz*16),maxZ=Math.min(b.z1(),cz*16+15);
        if(minX>maxX||minZ>maxZ)return List.of();
        var out=new ArrayList<Patch>();int w=maxX-minX+1,d=maxZ-minZ+1;
        out.add(new Patch(minX,b.y0(),minZ,w,1,d));out.add(new Patch(minX,b.y1(),minZ,w,1,d));
        int h=b.height()-2;
        if(b.z0()>=minZ&&b.z0()<=maxZ)out.add(new Patch(minX,b.y0()+1,b.z0(),w,h,1));
        if(b.z1()>=minZ&&b.z1()<=maxZ)out.add(new Patch(minX,b.y0()+1,b.z1(),w,h,1));
        int z0=Math.max(minZ,b.z0()+1),z1=Math.min(maxZ,b.z1()-1);
        if(z0<=z1){if(b.x0()>=minX&&b.x0()<=maxX)out.add(new Patch(b.x0(),b.y0()+1,z0,1,h,z1-z0+1));if(b.x1()>=minX&&b.x1()<=maxX)out.add(new Patch(b.x1(),b.y0()+1,z0,1,h,z1-z0+1));}
        return out;
    }
}
