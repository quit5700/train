package cn.quit5700.fabrictrain.cargo;

import java.util.UUID;

public record CargoTransfer(UUID transferId, long stationId, int requestedItems, int reservedItems) {
    public CargoTransfer {
        if (stationId <= 0 || requestedItems < 0 || reservedItems < 0 || reservedItems > requestedItems) {
            throw new IllegalArgumentException("Invalid cargo transfer");
        }
    }

    public static CargoTransfer reserve(long stationId, int requestedItems, int destinationCapacity) {
        int reserved = Math.min(Math.max(destinationCapacity, 0), Math.max(requestedItems, 0));
        return new CargoTransfer(UUID.randomUUID(), stationId, Math.max(requestedItems, 0), reserved);
    }
}
