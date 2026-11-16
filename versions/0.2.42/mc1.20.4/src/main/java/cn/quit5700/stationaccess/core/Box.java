package cn.quit5700.stationaccess.core;
import java.util.*;
public record Box(int x0,int y0,int z0,int x1,int y1,int z1) {
 public record Point(double x,double y,double z){}
 public record Cell(int x,int y,int z){}
 public static Box between(Cell a,Cell b){return new Box(Math.min(a.x,b.x),Math.min(a.y,b.y),Math.min(a.z,b.z),Math.max(a.x,b.x),Math.max(a.y,b.y),Math.max(a.z,b.z));}
 public int width(){return x1-x0+1;} public int height(){return y1-y0+1;} public int depth(){return z1-z0+1;}
 public boolean valid(){return width()>=3&&depth()>=3&&height()>=3;}
 public boolean large(){return width()>64||depth()>64||height()>32;}
 public Box inner(){return new Box(x0+1,y0+1,z0+1,x1-1,y1-1,z1-1);}
 public boolean contains(Point p){return p.x>=x0&&p.x<x1+1d&&p.y>=y0&&p.y<y1+1d&&p.z>=z0&&p.z<z1+1d;}
 public boolean contains(Cell p){return p.x>=x0&&p.x<=x1&&p.y>=y0&&p.y<=y1&&p.z>=z0&&p.z<=z1;}
 public boolean shell(Cell p){return contains(p)&&(p.x==x0||p.x==x1||p.y==y0||p.y==y1||p.z==z0||p.z==z1);}
 public boolean overlaps(Box b){return x0<=b.x1&&x1>=b.x0&&y0<=b.y1&&y1>=b.y0&&z0<=b.z1&&z1>=b.z0;}
 public List<Cell> crossings(Point a,Point b){
   double lo=0,hi=1;double[] from={a.x,a.y,a.z},to={b.x,b.y,b.z},min={x0,y0,z0},max={x1+1d,y1+1d,z1+1d};
   for(int axis=0;axis<3;axis++){double delta=to[axis]-from[axis];if(Math.abs(delta)<1e-10){if(from[axis]<min[axis]||from[axis]>=max[axis])return List.of();continue;}double t0=(min[axis]-from[axis])/delta,t1=(max[axis]-from[axis])/delta;if(t0>t1){double t=t0;t0=t1;t1=t;}lo=Math.max(lo,t0);hi=Math.min(hi,t1);if(lo>hi)return List.of();}
   if(hi-lo<1e-9)return List.of();var hits=new ArrayList<Cell>();if(!contains(a))hits.add(at(a,b,lo));if(!contains(b))hits.add(at(a,b,hi));return hits;
 }
 private static int clamp(int value,int min,int max){return Math.max(min,Math.min(value,max));}
 private Cell at(Point a,Point b,double t){return new Cell(clamp((int)Math.floor(a.x+(b.x-a.x)*t),x0,x1),clamp((int)Math.floor(a.y+(b.y-a.y)*t),y0,y1),clamp((int)Math.floor(a.z+(b.z-a.z)*t),z0,z1));}
}
