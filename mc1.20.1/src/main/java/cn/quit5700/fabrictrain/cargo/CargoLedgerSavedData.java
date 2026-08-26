package cn.quit5700.fabrictrain.cargo;

import cn.quit5700.fabrictrain.FabricTrainMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CargoLedgerSavedData extends SavedData {
    public static final Codec<CargoLedgerSavedData> CODEC = PendingDelivery.CODEC.listOf()
            .optionalFieldOf("deliveries", List.of()).codec()
            .xmap(CargoLedgerSavedData::new, data -> List.copyOf(data.deliveries));
    public static final String DATA_ID = FabricTrainMod.MOD_ID + "_cargo_ledger";

    private final ArrayList<PendingDelivery> deliveries;

    public CargoLedgerSavedData() { this(List.of()); }
    private CargoLedgerSavedData(List<PendingDelivery> deliveries) { this.deliveries = new ArrayList<>(deliveries); }

    public synchronized boolean containsSource(UUID sourceCarId) {
        return deliveries.stream().anyMatch(delivery -> delivery.sourceCarId().equals(sourceCarId));
    }

    public synchronized void add(PendingDelivery delivery) {
        if (deliveries.stream().anyMatch(existing -> existing.shipmentId().equals(delivery.shipmentId()))) return;
        deliveries.add(delivery);
        setDirty();
    }

    public synchronized List<PendingDelivery> deliveries() { return List.copyOf(deliveries); }

    public synchronized void replace(PendingDelivery oldDelivery, PendingDelivery updated) {
        int index = deliveries.indexOf(oldDelivery);
        if (index >= 0) {
            if (updated.stacks().isEmpty()) deliveries.remove(index); else deliveries.set(index, updated);
            setDirty();
        }
    }

    public static CargoLedgerSavedData load(CompoundTag tag) {
        return CODEC.parse(NbtOps.INSTANCE, tag)
                .result().orElseGet(CargoLedgerSavedData::new);
    }

    @Override public CompoundTag save(CompoundTag tag) {
        return CODEC.encodeStart(NbtOps.INSTANCE, this)
                .result().filter(CompoundTag.class::isInstance).map(CompoundTag.class::cast).orElse(tag);
    }
}
