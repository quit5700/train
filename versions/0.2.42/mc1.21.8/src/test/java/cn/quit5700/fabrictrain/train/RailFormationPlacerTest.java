package cn.quit5700.fabrictrain.train;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RailFormationPlacerTest {
    @Test void samplesSpacingAlongTurnsInsteadOfAcrossTheCorner() {
        List<Vec3> path = List.of(new Vec3(0, 0, 0), new Vec3(0, 0, 1), new Vec3(1, 0, 1));
        Vec3 target = RailFormationPlacer.sample(path, 1.5D);
        assertEquals(0.5D, target.x, 0.0001D);
        assertEquals(1.0D, target.z, 0.0001D);
    }
}
