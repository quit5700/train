package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.FabricTrainMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public final class OfflineTrainSavedData extends SavedData {
    public static final Codec<OfflineTrainSavedData> CODEC = OfflineTrainState.CODEC.listOf()
            .optionalFieldOf("trains", List.of()).codec()
            .xmap(OfflineTrainSavedData::new, data -> List.copyOf(data.trains.values()));
    public static final String DATA_ID = FabricTrainMod.MOD_ID + "_offline_trains";
    public static final SavedData.Factory<OfflineTrainSavedData> TYPE = new SavedData.Factory<>(
            OfflineTrainSavedData::new, OfflineTrainSavedData::load, DataFixTypes.LEVEL);

    private final Map<UUID, OfflineTrainState> trains = new LinkedHashMap<>();
    public OfflineTrainSavedData() {}
    private OfflineTrainSavedData(List<OfflineTrainState> values) {
        values.forEach(value -> trains.put(value.trainId(), value));
    }
    public synchronized void put(OfflineTrainState state) { trains.put(state.trainId(), state); setDirty(); }
    public synchronized void remove(UUID trainId) { if (trains.remove(trainId) != null) setDirty(); }
    public synchronized List<OfflineTrainState> trains() { return List.copyOf(trains.values()); }
    public synchronized OfflineTrainState get(UUID trainId) { return trains.get(trainId); }

    private static OfflineTrainSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), tag)
                .result().orElseGet(OfflineTrainSavedData::new);
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), this)
                .result().filter(CompoundTag.class::isInstance).map(CompoundTag.class::cast).orElse(tag);
    }
}
