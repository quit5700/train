package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.FabricTrainMod;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

public final class OfflineTrainSavedData extends SavedData {
    public static final Codec<OfflineTrainSavedData> CODEC = OfflineTrainState.CODEC.listOf()
            .optionalFieldOf("trains", List.of()).codec()
            .xmap(OfflineTrainSavedData::new, data -> List.copyOf(data.trains.values()));
    public static final SavedDataType<OfflineTrainSavedData> TYPE = new SavedDataType<>(
            FabricTrainMod.id("offline_trains"), OfflineTrainSavedData::new, CODEC, null);

    private final Map<UUID, OfflineTrainState> trains = new LinkedHashMap<>();
    public OfflineTrainSavedData() {}
    private OfflineTrainSavedData(List<OfflineTrainState> values) {
        values.forEach(value -> trains.put(value.trainId(), value));
    }
    private cn.quit5700.fabrictrain.offline.InfrastructureData infrastructure;
    public void attach(cn.quit5700.fabrictrain.offline.InfrastructureData value){infrastructure=value;}
    public synchronized void put(OfflineTrainState state) {
        var owned=infrastructure==null?null:infrastructure.trains.get(state.trainId());
        if(owned!=null){infrastructure.put(new cn.quit5700.fabrictrain.offline.InfrastructureData.Owned(owned.id(),owned.origin(),state,true,List.of(),owned.departure()));return;}
        if(infrastructure!=null&&infrastructure.retired.contains(state.trainId()))return;
        trains.put(state.trainId(), state); setDirty();
    }
    public synchronized void remove(UUID trainId) { if (trains.remove(trainId) != null) setDirty(); }
    public synchronized List<OfflineTrainState> trains() {
        var result=new LinkedHashMap<UUID,OfflineTrainState>(trains);
        if(infrastructure!=null){infrastructure.retired.forEach(result::remove);infrastructure.trains.values().forEach(t->{result.remove(t.id());if(t.virtual())result.put(t.id(),t.state());});}
        return List.copyOf(result.values());
    }
    public synchronized OfflineTrainState get(UUID trainId) { return trains().stream().filter(t->t.trainId().equals(trainId)).findFirst().orElse(null); }
}
