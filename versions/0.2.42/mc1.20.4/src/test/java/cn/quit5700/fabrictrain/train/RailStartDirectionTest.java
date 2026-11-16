package cn.quit5700.fabrictrain.train;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RailStartDirectionTest {
    @Test
    void choosesRailExitClosestToLocomotiveFront() {
        Vec3 result = RailStartDirection.choose(
                new Vec3i(-1, 0, 0),
                new Vec3i(1, 0, 0),
                new Vec3(0.8D, 0, 0.2D));

        assertEquals(1.0D, result.x, 0.0001D);
        assertEquals(0.0D, result.z, 0.0001D);
    }

    @Test
    void neverUsesOffRailFacingComponent() {
        Vec3 result = RailStartDirection.choose(
                new Vec3i(0, 0, -1),
                new Vec3i(0, 0, 1),
                new Vec3(0.9D, 0, -0.1D));

        assertEquals(0.0D, result.x, 0.0001D);
        assertEquals(-1.0D, result.z, 0.0001D);
    }
}
