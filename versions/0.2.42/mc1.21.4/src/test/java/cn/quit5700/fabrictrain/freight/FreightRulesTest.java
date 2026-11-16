package cn.quit5700.fabrictrain.freight;

import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.depot.DepotTemplate;
import cn.quit5700.fabrictrain.offline.InfrastructureData;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FreightRulesTest {
    private FreightYard newYard() {
        return new FreightYard(new InfrastructureData.Pos("minecraft:overworld", 0, 64, 0), 12);
    }

    @Test
    void oldAndNewDepotTemplatesPreserveCarPositions() {
        DepotTemplate previous = DepotTemplate.decode("PCPCC");
        assertEquals(5, previous.cars().size());
        assertEquals(0L, previous.destination(0));
        var configured = new DepotTemplate(previous.cars(), List.of(21L, 0L, 22L, 0L, 23L, 24L));
        var read = DepotTemplate.decode(configured.encode());
        assertEquals(configured, read);
        assertEquals(22L, read.destination(2));
        assertEquals(24L, read.destination(5));
        assertEquals(0L, read.filterDestinations(id -> id != 22).destination(2));
    }

    @Test
    void hiddenCarRulesAndLineMembershipSurviveReload() {
        var yard = newYard();
        yard.perCar = true;
        yard.lineOrder.addAll(List.of(8L, 4L));
        yard.filters(3).add("minecraft:iron_ingot");
        var root = new InfrastructureData();
        root.freightYards.put(yard.id, yard);
        var tag = InfrastructureData.CODEC.encodeStart(NbtOps.INSTANCE, root).result().orElseThrow();
        var restored = InfrastructureData.CODEC.parse(NbtOps.INSTANCE, tag).result().orElseThrow().freightYards.get(yard.id);
        assertTrue(restored.perCar);
        assertEquals(List.of(8L, 4L), restored.lineOrder);
        assertEquals(36, restored.page(3).getContainerSize());
        assertTrue(restored.filters(3).contains("minecraft:iron_ingot"));
    }

    @Test
    void returnedGoodsStayOutOfIntakeUntilRemoved() {
        var yard = newYard();
        var box = new InfrastructureData.Pos("minecraft:overworld", 0, 64, 1);
        yard.hold(box, "minecraft:iron_ingot", 12, 0);
        assertEquals(12, yard.held(box, "minecraft:iron_ingot", 15));
        assertEquals(3, yard.held(box, "minecraft:iron_ingot", 3));
        assertEquals(0, yard.held(box, "minecraft:iron_ingot", 0));
        assertEquals(0, yard.held(box, "minecraft:iron_ingot", 10));
    }
}
