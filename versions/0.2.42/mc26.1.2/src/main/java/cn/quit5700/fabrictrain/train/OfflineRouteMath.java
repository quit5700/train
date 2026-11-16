package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.route.TrackPoint;

import java.util.List;

public final class OfflineRouteMath {
    private OfflineRouteMath() {}

    public static Position advance(List<TrackPoint> path, boolean loop, int loopStart,
                                   int pathIndex, double progress, double distance) {
        if (path.size() < 2) return new Position(0, 0D, true);
        int index = Math.clamp(pathIndex, 0, path.size() - 1);
        double remaining = Math.max(0D, distance);
        double along = Math.max(0D, progress);
        int budget = Math.max(64, path.size() * 2);
        while (remaining > 0.000001D && budget-- > 0) {
            int next = nextIndex(path, loop, loopStart, index);
            if (next < 0) return new Position(index, 0D, true);
            double length = distance(path.get(index), path.get(next));
            double available = Math.max(0D, length - along);
            if (remaining <= available) return new Position(index, along + remaining, false);
            remaining -= available;
            index = next;
            along = 0D;
        }
        return new Position(index, along, false);
    }

    public static int nextIndex(List<TrackPoint> path, boolean loop, int loopStart, int index) {
        if (index + 1 < path.size()) return index + 1;
        return loop ? Math.clamp(loopStart, 0, path.size() - 2) : -1;
    }

    public static double distance(TrackPoint a, TrackPoint b) {
        double dx = b.x() - a.x(), dy = b.y() - a.y(), dz = b.z() - a.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static Pose pose(List<TrackPoint> path,boolean loop,int loopStart,int index,double progress) {
        if(path.size()<2)throw new IllegalArgumentException("A route needs at least two points");
        index=Math.max(0,Math.min(path.size()-1,index));
        int next=nextIndex(path,loop,loopStart,index);TrackPoint a=path.get(index);
        net.minecraft.world.phys.Vec3 from=new net.minecraft.world.phys.Vec3(a.x()+0.5D,a.y()+0.1D,a.z()+0.5D);
        if(next<0){TrackPoint previous=path.get(Math.max(0,index-1));var tangent=new net.minecraft.world.phys.Vec3(a.x()-previous.x(),a.y()-previous.y(),a.z()-previous.z()).normalize();return new Pose(from,tangent);}
        TrackPoint b=path.get(next);var direction=new net.minecraft.world.phys.Vec3(b.x()-a.x(),b.y()-a.y(),b.z()-a.z());
        double length=direction.length();var tangent=direction.normalize();return new Pose(from.add(tangent.scale(Math.max(0D,Math.min(length,progress)))),tangent);
    }
    public record Pose(net.minecraft.world.phys.Vec3 position,net.minecraft.world.phys.Vec3 tangent) {}

    public record Position(int pathIndex, double progress, boolean ended) {}
}
