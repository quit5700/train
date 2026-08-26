package cn.quit5700.fabrictrain.train;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

public final class RailStartDirection {
    private RailStartDirection() {}

    static Vec3 choose(Vec3i first, Vec3i second, Vec3 facing) {
        Vec3 firstDirection = horizontal(first);
        Vec3 secondDirection = horizontal(second);
        return firstDirection.dot(facing) >= secondDirection.dot(facing)
                ? firstDirection : secondDirection;
    }

    private static Vec3 horizontal(Vec3i step) {
        Vec3 direction = new Vec3(step.getX(), 0, step.getZ());
        double length = direction.horizontalDistance();
        return length < 0.0001D ? Vec3.ZERO : direction.scale(1.0D / length);
    }
}
