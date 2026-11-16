package cn.quit5700.fabrictrain.offline;

import cn.quit5700.fabrictrain.train.*;
import cn.quit5700.fabrictrain.route.TrackPoint;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class InfrastructurePersistenceTest {
    @Test void routeFingerprintHasCanonicalOrderAcrossJvmMapOrders() throws Exception {
        var method=InfrastructureService.class.getDeclaredMethod("signature",cn.quit5700.fabrictrain.data.RailwayLine.class);method.setAccessible(true);
        for(var choices:List.of(Map.of(0,1,1,1,2,1),Map.of(1,1,5,1,9,1))){
            var line=new cn.quit5700.fabrictrain.data.RailwayLine(1,"test",1,List.of(1L,2L),3,28.8,2,cn.quit5700.fabrictrain.data.LineStatus.RUNNING,choices,Map.of());
            assertEquals("1|[1, 2]|"+new TreeMap<>(choices)+"|{}",method.invoke(null,line));
        }
    }
    private OfflineTrainState train(UUID id) {
        return new OfflineTrainState(id,1,TrainClass.CARGO,28.8,0,.25,.4,false,0,1,0,0,150,100,
                List.of(new TrackPoint(1,64,1),new TrackPoint(1,64,2)),List.of());
    }
    @Test void stockAndVirtualOwnershipPersistInOneSnapshot() {
        var root=new InfrastructureData();var id=UUID.randomUUID();
        var p=new InfrastructureData.Pos("minecraft:overworld",12,64,-32);
        root.put(new InfrastructureData.Stock(p,"fabric_train_simulator:train_depot",54,64,List.of(),true));
        root.put(new InfrastructureData.Owned(id,3,train(id),true,List.of()));
        var encoded=InfrastructureData.CODEC.encodeStart(NbtOps.INSTANCE,root).result().orElseThrow();
        var restored=InfrastructureData.CODEC.parse(NbtOps.INSTANCE,encoded).result().orElseThrow();
        assertTrue(restored.stocks.get(p.key()).owned());
        assertEquals("fabric_train_simulator:train_depot",restored.stocks.get(p.key()).block());
        assertTrue(restored.trains.get(id).virtual());assertEquals(150,restored.trains.get(id).state().dwellUntilGameTime());
    }
    @Test void liveCheckpointIsNotAnOfflineDuplicate() {
        var id=UUID.randomUUID();var root=new InfrastructureData();var facade=new OfflineTrainSavedData();facade.attach(root);
        root.put(new InfrastructureData.Owned(id,3,train(id),false,List.of(UUID.randomUUID())));
        assertNull(facade.get(id));assertTrue(facade.trains().isEmpty());
        facade.put(train(id));assertNotNull(facade.get(id));assertEquals(1,facade.trains().size());
    }
    @Test void retiredOwnershipSuppressesOldLegacyState() {
        var id=UUID.randomUUID();var root=new InfrastructureData();var facade=new OfflineTrainSavedData();
        facade.put(train(id));facade.attach(root);root.retired.add(id);
        assertNull(facade.get(id));assertTrue(facade.trains().isEmpty());
        facade.put(train(id));assertTrue(facade.trains().isEmpty());
    }
    @Test void recoveryAndRetiredIdSurviveSameSave() {
        var id=UUID.randomUUID();var root=new InfrastructureData();root.retired.add(id);
        root.recoveries.put(id,new InfrastructureData.Recovery(id,new InfrastructureData.Pos("minecraft:overworld",1,64,2),List.of()));
        var encoded=InfrastructureData.CODEC.encodeStart(NbtOps.INSTANCE,root).result().orElseThrow();
        var restored=InfrastructureData.CODEC.parse(NbtOps.INSTANCE,encoded).result().orElseThrow();
        assertTrue(restored.retired.contains(id));assertTrue(restored.recoveries.containsKey(id));
    }
}
