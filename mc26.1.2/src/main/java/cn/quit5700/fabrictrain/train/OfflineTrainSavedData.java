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
    public synchronized void put(OfflineTrainState state) { trains.put(state.trainId(), state); setDirty(); }
    public synchronized void remove(UUID trainId) { if (trains.remove(trainId) != null) setDirty(); }
    public synchronized List<OfflineTrainState> trains() { return List.copyOf(trains.values()); }
    public synchronized OfflineTrainState get(UUID trainId) { return trains.get(trainId); }
}
