package cn.quit5700.fabrictrain.train;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

/** Actual edge-to-edge rail geometry for free/manual movement. */
final class RailSegmentGeometry {
    private RailSegmentGeometry() {}
    static Segment between(BlockPos rail, Vec3i entry, Vec3i exit) {
        boolean slope = entry.getY() != exit.getY();
        return new Segment(endpoint(rail, entry, slope), endpoint(rail, exit, slope));
    }
    private static Vec3 endpoint(BlockPos rail, Vec3i side, boolean slope) {
        return new Vec3(rail.getX() + .5D + side.getX() * .5D,
                rail.getY() + .1D + (slope && side.getY() >= 0 ? 1D : 0D),
                rail.getZ() + .5D + side.getZ() * .5D);
    }
    record Segment(Vec3 entry, Vec3 exit) {
        double length() { return entry.distanceTo(exit); }
        double project(Vec3 position) {
            Vec3 delta = exit.subtract(entry);
            return length() * Math.clamp(position.subtract(entry).dot(delta) / delta.lengthSqr(), 0D, 1D);
        }
        Vec3 at(double distance) { return entry.lerp(exit, Math.clamp(distance / length(), 0D, 1D)); }
        float yaw() { Vec3 delta = exit.subtract(entry); return (float)Math.toDegrees(Math.atan2(-delta.x, delta.z)); }
    }
}
