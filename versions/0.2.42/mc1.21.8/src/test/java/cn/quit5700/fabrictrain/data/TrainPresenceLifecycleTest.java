package cn.quit5700.fabrictrain.data;

import cn.quit5700.fabrictrain.train.ControlMode;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TrainPresenceLifecycleTest {
    @Test
    void onlyLoadedUnloadedAndOfflineTrainsAreVisible() {
        assertTrue(TrainPresence.LOADED.visible());
        assertTrue(TrainPresence.UNLOADED.visible());
        assertTrue(TrainPresence.OFFLINE.visible());
        assertFalse(TrainPresence.DESTROYED.visible());
        assertFalse(TrainPresence.LEGACY_UNCONFIRMED.visible());
    }

    @Test
    void upgradingOldPresenceFormatHidesUnconfirmedRecordsUntilEntityLoads() {
        net.minecraft.nbt.CompoundTag entry = new net.minecraft.nbt.CompoundTag();
        entry.putBoolean("Present", true);
        assertEquals(TrainPresence.LEGACY_UNCONFIRMED,
                TrainRecordPresence.readLifecycle(entry, 2));
    }

    @Test
    void observedEntityReactivatesItsStableNumberAfterOneTimeCleanup() {
        RailwaySavedData data = new RailwaySavedData();
        UUID id = UUID.randomUUID();
        long number = data.ensureTrain(id, false, 0L, ControlMode.MANUAL).number();
        data.updateTrainPresence(id, TrainPresence.LEGACY_UNCONFIRMED);
        assertFalse(data.presentTrainRecords().containsKey(id));
        TrainRecord loaded = data.ensureTrain(id, false, 0L, ControlMode.MANUAL);
        assertEquals(number, loaded.number());
        assertEquals(TrainPresence.LOADED, loaded.presence());
    }

    @Test
    void serverRestartOnlyHidesRecordsThatClaimToStillBeLoaded() {
        RailwaySavedData data = new RailwaySavedData();
        UUID staleLoaded = UUID.randomUUID();
        UUID unloaded = UUID.randomUUID();
        UUID offline = UUID.randomUUID();
        data.ensureTrain(staleLoaded, false, 0L, ControlMode.MANUAL);
        data.ensureTrain(unloaded, true, 7L, ControlMode.ROUTE_AUTOMATIC);
        data.ensureTrain(offline, true, 7L, ControlMode.ROUTE_AUTOMATIC);
        data.updateTrainPresence(unloaded, TrainPresence.UNLOADED);
        data.updateTrainPresence(offline, TrainPresence.OFFLINE);

        data.prepareTrainRegistryForRestart();

        assertEquals(TrainPresence.LEGACY_UNCONFIRMED, data.trainRecord(staleLoaded).presence());
        assertEquals(TrainPresence.UNLOADED, data.trainRecord(unloaded).presence());
        assertEquals(TrainPresence.OFFLINE, data.trainRecord(offline).presence());
    }

    @Test
    void depotLineListOnlyContainsVisibleAutomaticTrainsForThatLine() {
        RailwaySavedData data = new RailwaySavedData();
        UUID manual = UUID.randomUUID();
        UUID selectedAutomatic = UUID.randomUUID();
        UUID otherAutomatic = UUID.randomUUID();
        data.ensureTrain(manual, false, 0L, ControlMode.MANUAL);
        data.ensureTrain(selectedAutomatic, true, 7L, ControlMode.ROUTE_AUTOMATIC);
        data.ensureTrain(otherAutomatic, true, 8L, ControlMode.ROUTE_AUTOMATIC);
        data.replaceTrainManifest(manual, 0L, java.util.List.of(new TrainMember(
                UUID.randomUUID(), TrainVehicleKind.PASSENGER, 1)));
        data.replaceTrainManifest(selectedAutomatic, 2L, java.util.List.of(new TrainMember(
                UUID.randomUUID(), TrainVehicleKind.LOCOMOTIVE, 1)));
        data.replaceTrainManifest(otherAutomatic, 3L, java.util.List.of(new TrainMember(
                UUID.randomUUID(), TrainVehicleKind.LOCOMOTIVE, 1)));
        data.updateTrainPresence(selectedAutomatic, TrainPresence.OFFLINE);

        assertEquals(java.util.Set.of(selectedAutomatic), data.presentAutomaticTrainRecords(7L).keySet());
        assertTrue(data.presentAutomaticTrainRecords(0L).isEmpty());
    }

    @Test
    void serverStartupUsesLifecycleAwareRegistryPreparation() throws Exception {
        String initializer = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/FabricTrainMod.java"), StandardCharsets.UTF_8);
        assertTrue(initializer.contains("prepareTrainRegistryForRestart()"));
        assertFalse(initializer.contains("clearActiveTrainsForRestart()"));
    }
}
