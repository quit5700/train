package cn.quit5700.fabrictrain.train;

import net.minecraft.world.phys.Vec3;

public final class FormationGeometry {
    private static final double CONTACT_DISTANCE_SQUARED = 2.0D;
    private FormationGeometry() {}
    public static boolean isContact(double distanceSquared) { return distanceSquared <= CONTACT_DISTANCE_SQUARED; }
    public static Vec3 anchor(Vec3 leader, Vec3 follower, double spacing) {
        Vec3 offset = follower.subtract(leader);
        if (offset.horizontalDistanceSqr() < 0.0001D) offset = new Vec3(0, 0, 1);
        Vec3 horizontal = new Vec3(offset.x, 0, offset.z).normalize().scale(spacing);
        return new Vec3(leader.x + horizontal.x, leader.y, leader.z + horizontal.z);
    }
}
