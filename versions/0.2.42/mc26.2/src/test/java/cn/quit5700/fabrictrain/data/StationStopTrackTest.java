package cn.quit5700.fabrictrain.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StationStopTrackTest {
    @Test void stopTrackBelongsToOneStationOccurrenceAndSurvivesReordering() {
        RailwaySavedData data = new RailwaySavedData();
        RailwayDevice first = data.register(DeviceType.STATION, "minecraft:overworld", 1, 60, 1);
        RailwayDevice second = data.register(DeviceType.STATION, "minecraft:overworld", 8, 72, 8);
        RailwayLine line = data.createEmptyLine("高低站台线");
        data.insertLineNode(line.id(), 0, first.id());
        data.insertLineNode(line.id(), 1, second.id());

        data.setStationStopTrack(line.id(), 0, new StopTrack(2, 64, 1));
        data.setStationStopTrack(line.id(), 1, new StopTrack(9, 81, 8));
        RailwayLine moved = data.modifyLineNode(line.id(), 1, 0);

        assertEquals(new StopTrack(9, 81, 8), moved.stationStopTrack(0));
        assertEquals(new StopTrack(2, 64, 1), moved.stationStopTrack(1));
    }

    @Test void repeatedStationMayUseDifferentStopTracks() {
        RailwaySavedData data = new RailwaySavedData();
        RailwayDevice station = data.register(DeviceType.STATION, "minecraft:overworld", 1, 60, 1);
        RailwayLine line = data.createEmptyLine("循环站台线");
        data.insertLineNode(line.id(), 0, station.id());
        data.insertLineNode(line.id(), 1, station.id());
        data.setStationStopTrack(line.id(), 0, new StopTrack(2, 64, 1));
        RailwayLine updated = data.setStationStopTrack(line.id(), 1, new StopTrack(2, 90, 1));

        assertEquals(new StopTrack(2, 64, 1), updated.stationStopTrack(0));
        assertEquals(new StopTrack(2, 90, 1), updated.stationStopTrack(1));
    }

    @Test void removingNodeRemovesOnlyItsBindingAndShiftsLaterBindings() {
        RailwaySavedData data = new RailwaySavedData();
        RailwayDevice a = data.register(DeviceType.STATION, "minecraft:overworld", 1, 60, 1);
        RailwayDevice b = data.register(DeviceType.STATION, "minecraft:overworld", 2, 60, 1);
        RailwayDevice c = data.register(DeviceType.STATION, "minecraft:overworld", 3, 60, 1);
        RailwayLine line = data.createEmptyLine("删除站台线");
        data.insertLineNode(line.id(), 0, a.id());
        data.insertLineNode(line.id(), 1, b.id());
        data.insertLineNode(line.id(), 2, c.id());
        data.setStationStopTrack(line.id(), 0, new StopTrack(1, 61, 1));
        data.setStationStopTrack(line.id(), 1, new StopTrack(2, 61, 1));
        data.setStationStopTrack(line.id(), 2, new StopTrack(3, 61, 1));

        RailwayLine updated = data.modifyLineNode(line.id(), 1, 2);

        assertEquals(new StopTrack(1, 61, 1), updated.stationStopTrack(0));
        assertEquals(new StopTrack(3, 61, 1), updated.stationStopTrack(1));
        assertNull(updated.stationStopTrack(2));
    }
}
