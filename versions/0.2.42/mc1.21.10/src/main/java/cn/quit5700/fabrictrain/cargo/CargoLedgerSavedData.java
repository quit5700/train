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
    private cn.quit5700.fabrictrain.offline.InfrastructureData infrastructure;
    public void attach(cn.quit5700.fabrictrain.offline.InfrastructureData value){infrastructure=value;}
    public boolean managed(PendingDelivery delivery){return infrastructure!=null&&infrastructure.deliveries.containsKey(delivery.shipmentId());}

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

    public synchronized List<PendingDelivery> deliveries() { var result=new ArrayList<>(deliveries);if(infrastructure!=null)result.addAll(infrastructure.deliveries.values());return List.copyOf(result); }

    public synchronized void replace(PendingDelivery oldDelivery, PendingDelivery updated) {
        if(managed(oldDelivery)) {
            if(updated.stacks().isEmpty())infrastructure.deliveries.remove(oldDelivery.shipmentId());else infrastructure.deliveries.put(updated.shipmentId(),updated);
            infrastructure.setDirty();return;
        }
        int index = deliveries.indexOf(oldDelivery);
        if (index >= 0) {
            if (updated.stacks().isEmpty()) deliveries.remove(index); else deliveries.set(index, updated);
            setDirty();
        }
    }
}
