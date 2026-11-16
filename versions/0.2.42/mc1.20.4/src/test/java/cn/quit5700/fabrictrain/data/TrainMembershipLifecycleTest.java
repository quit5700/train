package cn.quit5700.fabrictrain.data;

import cn.quit5700.fabrictrain.compat.NbtCompat;

import cn.quit5700.fabrictrain.train.ControlMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;

import static org.junit.jupiter.api.Assertions.*;

class TrainMembershipLifecycleTest {
    @Test void manifestAndOriginDepotSurviveWorldSaveRoundTrip() {
        RailwaySavedData original = new RailwaySavedData();
        UUID trainId = UUID.randomUUID();
        UUID locomotive = UUID.randomUUID();
        UUID passenger = UUID.randomUUID();
        long number = original.ensureTrain(trainId, true, 9L, ControlMode.ROUTE_AUTOMATIC).number();
        original.replaceTrainManifest(trainId, 2L, List.of(
                new TrainMember(locomotive, TrainVehicleKind.LOCOMOTIVE, 1),
                new TrainMember(passenger, TrainVehicleKind.PASSENGER, 2)));

        var encoded = RailwaySavedData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow(false, message -> {});
        RailwaySavedData restored = RailwaySavedData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow(false, message -> {});

        assertEquals(number, restored.trainRecord(trainId).number());
        assertEquals(2L, restored.trainManifest(trainId).originDepotNumber());
        assertEquals(List.of(locomotive, passenger), restored.trainManifest(trainId).members().stream()
                .map(TrainMember::entityId).toList());
    }

    @Test void duplicateNumbersInOldSaveAreMigratedToUniqueNumbers() {
        RailwaySavedData original = new RailwaySavedData();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        long duplicate = original.ensureTrain(first, false, 0L, ControlMode.MANUAL).number();
        original.ensureTrain(second, false, 0L, ControlMode.MANUAL);
        CompoundTag encoded = (CompoundTag) RailwaySavedData.CODEC
                .encodeStart(NbtOps.INSTANCE, original).getOrThrow(false, message -> {});
        ListTag registry = NbtCompat.getList(encoded, "TrainRegistry");
        NbtCompat.getCompound(registry, 1).putLong("Number", duplicate);

        RailwaySavedData restored = RailwaySavedData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow(false, message -> {});
        assertNotEquals(restored.trainRecord(first).number(), restored.trainRecord(second).number());
    }

    @Test void trainNumbersAreNeverReusedAfterRetirement() {
        RailwaySavedData data = new RailwaySavedData();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        long firstNumber = data.ensureTrain(first, false, 0L, ControlMode.MANUAL).number();
        data.removeTrainRecord(first);
        long secondNumber = data.ensureTrain(second, false, 0L, ControlMode.MANUAL).number();
        assertTrue(secondNumber > firstNumber);
        assertNotEquals(firstNumber, secondNumber);
    }

    @Test void oneRemovedVehicleKeepsRecordButLastVehicleRetiresIt() {
        RailwaySavedData data = new RailwaySavedData();
        UUID trainId = UUID.randomUUID();
        UUID locomotive = UUID.randomUUID();
        UUID cargo = UUID.randomUUID();
        data.ensureTrain(trainId, false, 0L, ControlMode.MANUAL);
        data.replaceTrainManifest(trainId, 0L, List.of(
                new TrainMember(locomotive, TrainVehicleKind.LOCOMOTIVE, 1),
                new TrainMember(cargo, TrainVehicleKind.CARGO, 2)));

        assertEquals(RailwaySavedData.MemberRemovalResult.MEMBERS_REMAIN,
                data.unregisterTrainMember(trainId, cargo));
        assertTrue(data.presentTrainRecords().containsKey(trainId));
        assertEquals(1, data.trainManifest(trainId).totalVehicles());

        assertEquals(RailwaySavedData.MemberRemovalResult.TRAIN_EMPTY,
                data.unregisterTrainMember(trainId, locomotive));
        assertFalse(data.presentTrainRecords().containsKey(trainId));
        assertNull(data.trainManifest(trainId));
        assertEquals(TrainPresence.DESTROYED, data.trainRecord(trainId).presence());
    }

    @Test void depotListIncludesManualAndOnlyCurrentLinesAutomaticTrains() {
        RailwaySavedData data = new RailwaySavedData();
        UUID manual = registered(data, ControlMode.MANUAL, 0L);
        UUID currentAutomatic = registered(data, ControlMode.ROUTE_AUTOMATIC, 3L);
        UUID otherAutomatic = registered(data, ControlMode.ROUTE_AUTOMATIC, 4L);

        assertEquals(java.util.Set.of(manual, currentAutomatic),
                data.presentDepotTrainRecords(3L).keySet());
        assertFalse(data.presentDepotTrainRecords(3L).containsKey(otherAutomatic));
    }

    private static UUID registered(RailwaySavedData data, ControlMode mode, long lineId) {
        UUID trainId = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        data.ensureTrain(trainId, mode == ControlMode.ROUTE_AUTOMATIC, lineId, mode);
        data.replaceTrainManifest(trainId, 2L,
                List.of(new TrainMember(member, TrainVehicleKind.PASSENGER, 1)));
        return trainId;
    }
}

