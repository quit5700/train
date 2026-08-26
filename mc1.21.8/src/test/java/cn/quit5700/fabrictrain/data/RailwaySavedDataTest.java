package cn.quit5700.fabrictrain.data;

import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import cn.quit5700.fabrictrain.train.ControlMode;

import static org.junit.jupiter.api.Assertions.*;

class RailwaySavedDataTest {
    @Test
    void trainRegistryPreservesUniqueNumberAndDepotOrigin() {
        RailwaySavedData original = new RailwaySavedData();
        UUID manualId = UUID.randomUUID();
        UUID depotId = UUID.randomUUID();
        TrainRecord manual = original.ensureTrain(manualId, false, 0L);
        TrainRecord depot = original.ensureTrain(depotId, true, 7L);

        var encoded = RailwaySavedData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        RailwaySavedData restored = RailwaySavedData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();

        assertEquals(manual.number(), restored.trainRecord(manualId).number());
        assertFalse(restored.trainRecord(manualId).depotGenerated());
        assertEquals(depot.number(), restored.trainRecord(depotId).number());
        assertTrue(restored.trainRecord(depotId).depotGenerated());
        assertNotEquals(manual.number(), depot.number());
    }

    @Test void depotOriginAndControlModeAreIndependentAndPersistent(){
        RailwaySavedData original=new RailwaySavedData();UUID immediate=UUID.randomUUID(),scheduled=UUID.randomUUID();
        original.ensureTrain(immediate,true,0L,ControlMode.MANUAL);
        original.ensureTrain(scheduled,true,7L,ControlMode.ROUTE_AUTOMATIC);
        var encoded=RailwaySavedData.CODEC.encodeStart(NbtOps.INSTANCE,original).getOrThrow();
        RailwaySavedData restored=RailwaySavedData.CODEC.parse(NbtOps.INSTANCE,encoded).getOrThrow();
        assertTrue(restored.trainRecord(immediate).depotGenerated());
        assertEquals(ControlMode.MANUAL,restored.trainRecord(immediate).controlMode());
        assertEquals(ControlMode.ROUTE_AUTOMATIC,restored.trainRecord(scheduled).controlMode());
    }

    @Test void retiredTrainDisappearsFromCurrentListButCanRecoverItsNumber() {
        RailwaySavedData data = new RailwaySavedData();
        UUID trainId = UUID.randomUUID();
        long number = data.ensureTrain(trainId, false, 0L, ControlMode.MANUAL).number();

        data.retireTrain(trainId);

        assertFalse(data.trainRecord(trainId).present());
        assertFalse(data.presentTrainRecords().containsKey(trainId));
        TrainRecord restored = data.ensureTrain(trainId, false, 0L, ControlMode.MANUAL);
        assertTrue(restored.present());
        assertEquals(number, restored.number());
    }

    @Test void legacyRecordWithoutPresenceMarkerStartsHiddenUntilItsEntityLoads() {
        net.minecraft.nbt.CompoundTag legacy = new net.minecraft.nbt.CompoundTag();
        assertFalse(TrainRecordPresence.read(legacy));
        legacy.putBoolean("Present", true);
        assertTrue(TrainRecordPresence.read(legacy));
        assertFalse(TrainRecordPresence.read(legacy, 1));
        assertTrue(TrainRecordPresence.read(legacy, TrainRecordPresence.CURRENT_FORMAT));
    }

    @Test void pendingDeletionCannotBeReactivatedByALateLoadingEntity() {
        RailwaySavedData data = new RailwaySavedData();
        UUID trainId = UUID.randomUUID();
        long number = data.ensureTrain(trainId, false, 0L, ControlMode.MANUAL).number();
        data.markTrainForDeletion(trainId);

        assertTrue(data.isTrainPendingDeletion(trainId));
        assertFalse(data.presentTrainRecords().containsKey(trainId));
        TrainRecord unchanged = data.ensureTrain(trainId, false, 0L, ControlMode.MANUAL);
        assertFalse(unchanged.present());
        assertEquals(number, unchanged.number());
    }
    @Test
    void newDevicesUseChineseSequentialDefaultNames() {
        RailwaySavedData data = new RailwaySavedData();
        assertEquals("车辆段1", data.register(DeviceType.DEPOT, "minecraft:overworld", 0, 64, 1).name());
        assertEquals("站点1", data.register(DeviceType.STATION, "minecraft:overworld", 1, 64, 1).name());
        assertEquals("道岔1", data.register(DeviceType.SWITCH, "minecraft:overworld", 2, 64, 1).name());
        assertEquals("站点2", data.register(DeviceType.STATION, "minecraft:overworld", 3, 64, 1).name());
    }
    @Test
    void deviceIdsSurviveSerializationAndAreNeverReused() {
        RailwaySavedData original = new RailwaySavedData();
        RailwayDevice station = original.register(DeviceType.STATION, "minecraft:overworld", 1, 64, 2);
        RailwayDevice depot = original.register(DeviceType.DEPOT, "minecraft:overworld", 5, 64, 2);
        original.remove("minecraft:overworld", 1, 64, 2);

        var encoded = RailwaySavedData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        RailwaySavedData restored = RailwaySavedData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        RailwayDevice next = restored.register(DeviceType.SWITCH, "minecraft:overworld", 8, 64, 2);

        assertEquals(1L, station.id());
        assertEquals(2L, depot.id());
        assertEquals(3L, next.id());
        assertNull(restored.find("minecraft:overworld", 1, 64, 2));
        assertNotNull(restored.find("minecraft:overworld", 5, 64, 2));
    }

    @Test
    void lineOrderAndNextIdSurviveSerialization() {
        RailwaySavedData original = new RailwaySavedData();
        RailwayDevice depot = original.register(DeviceType.DEPOT, "minecraft:overworld", 0, 64, 0);
        RailwayDevice station = original.register(DeviceType.STATION, "minecraft:overworld", 10, 64, 0);
        RailwayLine first = original.createLine(depot.id());
        original.appendLineNode(first.id(), station.id());
        original.deleteLine(first.id());

        var encoded = RailwaySavedData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        RailwaySavedData restored = RailwaySavedData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        RailwayLine second = restored.createLine(depot.id());
        RailwayLine updated = restored.appendLineNode(second.id(), station.id());

        assertEquals(2L, second.id());
        assertEquals(java.util.List.of(station.id()), updated.nodeIds());
        assertEquals(LineStatus.NEEDS_VALIDATION, updated.status());
    }

    @Test
    void lineCannotCrossDimensions() {
        RailwaySavedData data = new RailwaySavedData();
        RailwayDevice depot = data.register(DeviceType.DEPOT, "minecraft:overworld", 0, 64, 0);
        RailwayDevice station = data.register(DeviceType.STATION, "minecraft:the_nether", 0, 64, 0);
        RailwayLine line = data.createLine(depot.id());
        assertThrows(IllegalArgumentException.class, () -> data.appendLineNode(line.id(), station.id()));
    }

    @Test
    void activeTrainRegistrySurvivesSerializationAndCanReset() {
        RailwaySavedData original = new RailwaySavedData();
        RailwayDevice depot = original.register(DeviceType.DEPOT, "minecraft:overworld", 0, 64, 0);
        RailwayLine line = original.createLine(depot.id());
        java.util.UUID trainId = java.util.UUID.randomUUID();
        original.registerActiveTrain(trainId, line.id());

        var encoded = RailwaySavedData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        RailwaySavedData restored = RailwaySavedData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(1L, restored.activeTrainCount(line.id()));
        restored.clearActiveTrainsForRestart();
        assertEquals(0L, restored.activeTrainCount(line.id()));
    }

    @Test
    void lineNodesCanMoveAndDeleteWithoutChangingDispatchDepot() {
        RailwaySavedData data = new RailwaySavedData();
        RailwayDevice depot = data.register(DeviceType.DEPOT, "minecraft:overworld", 0, 64, 0);
        RailwayDevice first = data.register(DeviceType.STATION, "minecraft:overworld", 10, 64, 0);
        RailwayDevice second = data.register(DeviceType.STATION, "minecraft:overworld", 20, 64, 0);
        RailwayLine line = data.createLine(depot.id());
        data.appendLineNode(line.id(), first.id());
        data.appendLineNode(line.id(), second.id());

        assertEquals(java.util.List.of(second.id(), first.id()), data.modifyLineNode(line.id(), 1, 0).nodeIds());
        assertEquals(java.util.List.of(first.id()), data.modifyLineNode(line.id(), 0, 2).nodeIds());
        assertEquals(depot.id(), data.line(line.id()).depotId());
        assertEquals(LineStatus.NEEDS_VALIDATION, data.line(line.id()).status());
    }

    @Test
    void dispatchDepotBindingIsIndependentFromTheSingleRecoveryDepotNode() {
        RailwaySavedData data = new RailwaySavedData();
        RailwayDevice dispatchA = data.register(DeviceType.DEPOT, "minecraft:overworld", 0, 64, 0);
        RailwayDevice dispatchB = data.register(DeviceType.DEPOT, "minecraft:overworld", 5, 64, 0);
        RailwayDevice recovery = data.register(DeviceType.DEPOT, "minecraft:overworld", 20, 64, 0);
        RailwayDevice station = data.register(DeviceType.STATION, "minecraft:overworld", 10, 64, 0);
        RailwayLine first = data.createEmptyLine("一号线");
        RailwayLine second = data.createEmptyLine("二号线");
        data.insertLineNode(first.id(), 0, station.id());
        data.insertLineNode(first.id(), 1, recovery.id());

        assertEquals(0L, data.line(first.id()).depotId(), "添加回收节点不能偷偷成为发车车辆段");
        assertEquals(recovery.id(), data.recoveryDepotId(first.id()));
        assertEquals(0L, data.bindDispatchDepot(dispatchA.id(), first.id()));
        assertEquals(dispatchA.id(), data.line(first.id()).depotId());
        assertEquals(first.id(), data.dispatchLineIdForDepot(dispatchA.id()));

        assertEquals(dispatchA.id(), data.bindDispatchDepot(dispatchB.id(), first.id()),
                "新车辆段绑定同一线路时应返回并解除旧发车车辆段");
        assertEquals(0L, data.dispatchLineIdForDepot(dispatchA.id()));
        assertEquals(first.id(), data.dispatchLineIdForDepot(dispatchB.id()));

        data.bindDispatchDepot(dispatchB.id(), second.id());
        assertEquals(0L, data.line(first.id()).depotId(), "同一车辆段改绑后旧线路必须解除");
        assertEquals(dispatchB.id(), data.line(second.id()).depotId());
        data.bindDispatchDepot(dispatchB.id(), 0L);
        assertEquals(0L, data.line(second.id()).depotId());
    }

    @Test
    void lineAllowsOnlyOneRecoveryDepotButDispatcherMayBeThatSameDepot() {
        RailwaySavedData data = new RailwaySavedData();
        RailwayDevice firstDepot = data.register(DeviceType.DEPOT, "minecraft:overworld", 0, 64, 0);
        RailwayDevice secondDepot = data.register(DeviceType.DEPOT, "minecraft:overworld", 10, 64, 0);
        RailwayLine line = data.createEmptyLine("环线");
        data.insertLineNode(line.id(), 0, firstDepot.id());

        assertThrows(IllegalArgumentException.class,
                () -> data.insertLineNode(line.id(), 1, secondDepot.id()));
        assertDoesNotThrow(() -> data.bindDispatchDepot(firstDepot.id(), line.id()));
        assertEquals(firstDepot.id(), data.line(line.id()).depotId());
        assertEquals(firstDepot.id(), data.recoveryDepotId(line.id()));
    }

    @Test
    void configurableClassSpeedsSurviveSerialization() {
        RailwaySavedData original = new RailwaySavedData();
        original.updateClassSpeedLimits(30.0D, 130.0D, 200.0D);
        var encoded = RailwaySavedData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        RailwaySavedData restored = RailwaySavedData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(30.0D, restored.classSpeedLimit(cn.quit5700.fabrictrain.train.TrainClass.CARGO));
        assertEquals(130.0D, restored.classSpeedLimit(cn.quit5700.fabrictrain.train.TrainClass.PASSENGER));
        assertEquals(200.0D, restored.classSpeedLimit(cn.quit5700.fabrictrain.train.TrainClass.EXPRESS));
    }

    @Test
    void depotOwnsOnlyOneLine() {
        RailwaySavedData data = new RailwaySavedData();
        RailwayDevice depot = data.register(DeviceType.DEPOT, "minecraft:overworld", 0, 64, 0);
        RailwayLine first = data.createLine(depot.id());
        RailwayLine second = data.createLine(depot.id());
        assertEquals(first.id(), second.id());
        assertEquals(1, data.lines().size());
    }

    @Test void announcementAndPerLineSwitchDirectionSurviveSerialization(){
        RailwaySavedData original=new RailwaySavedData();RailwayDevice railwaySwitch=original.register(DeviceType.SWITCH,"minecraft:overworld",1,64,1);RailwayDevice station=original.register(DeviceType.STATION,"minecraft:overworld",2,64,1);RailwayLine line=original.createEmptyLine("测试线");original.insertLineNode(line.id(),0,railwaySwitch.id());original.insertLineNode(line.id(),1,station.id());original.setSwitchDirection(line.id(),0,2);original.setStationStopTrack(line.id(),1,new StopTrack(8,93,-4));original.updateDevice(station.id(),station.name(),30,1,"欢迎到站");original.updateCargoDwell(station.id(), 145);
        var encoded=RailwaySavedData.CODEC.encodeStart(NbtOps.INSTANCE,original).getOrThrow();RailwaySavedData restored=RailwaySavedData.CODEC.parse(NbtOps.INSTANCE,encoded).getOrThrow();
        assertEquals("欢迎到站",restored.device(station.id()).announcement());assertEquals(145,restored.device(station.id()).cargoDwellSeconds());assertEquals(2,restored.line(line.id()).switchDirection(0));assertEquals(new StopTrack(8,93,-4),restored.line(line.id()).stationStopTrack(1));
    }
}
