package cn.quit5700.fabrictrain.cargo;

import cn.quit5700.fabrictrain.FabricTrainMod;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CargoLedgerSavedData extends SavedData {
    public static final Codec<CargoLedgerSavedData> CODEC = PendingDelivery.CODEC.listOf()
            .optionalFieldOf("deliveries", List.of()).codec()
            .xmap(CargoLedgerSavedData::new, data -> List.copyOf(data.deliveries));
    public static final SavedDataType<CargoLedgerSavedData> TYPE = new SavedDataType<>(
            FabricTrainMod.MOD_ID + "_cargo_ledger", CargoLedgerSavedData::new, CODEC, null);

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
}
