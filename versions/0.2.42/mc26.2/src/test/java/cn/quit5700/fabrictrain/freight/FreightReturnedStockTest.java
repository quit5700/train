package cn.quit5700.fabrictrain.freight;

import cn.quit5700.fabrictrain.offline.InfrastructureData;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FreightReturnedStockTest {
    private final InfrastructureData.Pos box = new InfrastructureData.Pos("minecraft:overworld", -1, -61, 23);
    private FreightYard yard() { return new FreightYard(new InfrastructureData.Pos("minecraft:overworld", 0, -61, 23), 13); }

    @Test void reactivatedCarRuleAdmitsPreviouslyReturnedStock() {
        var yard = yard();
        yard.filters(1).add("minecraft:diamond");
        yard.hold(box, "minecraft:diamond", 64, 0);
        assertFalse(yard.releaseAcceptedReturns(box, 0));
        assertEquals(64, yard.held(box, "minecraft:diamond", 64));
        assertTrue(yard.releaseAcceptedReturns(box, 3));
        assertEquals(0, yard.held(box, "minecraft:diamond", 64));
        assertFalse(yard.releaseAcceptedReturns(box, 3));
    }

    @Test void hiddenRulesAndUnselectedItemsKeepReturnProtection() {
        var yard = yard();
        yard.filters(3).add("minecraft:diamond");
        yard.hold(box, "minecraft:diamond", 64, 0);
        yard.hold(box, "minecraft:dirt", 32, 0);
        assertFalse(yard.releaseAcceptedReturns(box, 2));
        assertTrue(yard.releaseAcceptedReturns(box, 3));
        assertEquals(32, yard.held(box, "minecraft:dirt", 32));
    }

    @Test void restoringMatchingRuleAfterReloadReleasesOnlyThatSource() {
        var yard = yard();
        var other = new InfrastructureData.Pos("minecraft:overworld", -2, -60, 22);
        yard.filters(1).add("minecraft:diamond");
        yard.hold(box, "minecraft:diamond", 64, 0);
        yard.hold(other, "minecraft:diamond", 8, 0);
        var root = new InfrastructureData();root.freightYards.put(yard.id, yard);
        var tag = InfrastructureData.CODEC.encodeStart(NbtOps.INSTANCE, root).result().orElseThrow();
        var restored = InfrastructureData.CODEC.parse(NbtOps.INSTANCE, tag).result().orElseThrow().freightYards.get(yard.id);
        assertTrue(restored.releaseAcceptedReturns(box, 1));
        assertEquals(0, restored.held(box, "minecraft:diamond", 64));
        assertEquals(8, restored.held(other, "minecraft:diamond", 8));
    }
}
