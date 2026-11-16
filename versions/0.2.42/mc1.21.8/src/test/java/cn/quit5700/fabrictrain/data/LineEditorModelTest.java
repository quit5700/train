package cn.quit5700.fabrictrain.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LineEditorModelTest {
    @Test void emptyLineAcceptsDraggedDevicesAndPerLineSwitchDirection() {
        RailwaySavedData data = new RailwaySavedData();
        RailwayDevice station = data.register(DeviceType.STATION, "minecraft:overworld", 0, 64, 0);
        RailwayDevice railwaySwitch = data.register(DeviceType.SWITCH, "minecraft:overworld", 1, 64, 0);
        RailwayDevice depot = data.register(DeviceType.DEPOT, "minecraft:overworld", 2, 64, 0);
        RailwayLine line = data.createEmptyLine("一号线");
        assertTrue(line.nodeIds().isEmpty());
        data.insertLineNode(line.id(), 0, station.id());
        data.insertLineNode(line.id(), 1, railwaySwitch.id());
        line = data.insertLineNode(line.id(), 2, depot.id());
        assertEquals(java.util.List.of(station.id(), railwaySwitch.id(), depot.id()), line.nodeIds());
        assertEquals(1, line.switchDirection(1));
        line = data.setSwitchDirection(line.id(), 1, 2);
        assertEquals(2, line.switchDirection(1));
        line = data.modifyLineNode(line.id(), 1, 1);
        assertEquals(2, line.switchDirection(2));
        line = data.modifyLineNode(line.id(), 2, 2);
        assertEquals(-1, line.switchDirection(1));
    }
}
