package cn.quit5700.fabrictrain.train;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FormationGeometryTest {
    @Test void newlyLinkedFollowerIsAnchoredAtConfiguredSpacing() {
        Vec3 anchor = FormationGeometry.anchor(new Vec3(10, 64, 10), new Vec3(13, 64, 10), 1.25);
        assertEquals(1.25, anchor.distanceTo(new Vec3(10, 64, 10)), 0.0001);
        assertEquals(11.25, anchor.x, 0.0001);
    }
    @Test void touchingCarsConnectWithoutOldSpeedGate() {
        assertTrue(FormationGeometry.isContact(1.79));
        assertFalse(FormationGeometry.isContact(2.01));
    }
}
