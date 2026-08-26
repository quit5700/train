package cn.quit5700.fabrictrain.route;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldLineValidatorContractTest {
    @Test void eachSegmentUsesTheDirectionSavedForItsSourceOccurrence() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/route/WorldLineValidator.java"));
        assertTrue(source.contains("line.switchDirection(i - 1)"));
        assertTrue(source.contains("segmentNeighbours"));
        assertTrue(source.contains("DeviceRailBlock.ENTRY_DIRECTION"));
    }

    @Test void stationStopIsExactlyTheSignalRailAboveTheStationBlock() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/route/WorldLineValidator.java"));
        assertTrue(source.contains("StationStopTrackRules.directlyAbove"));
        assertTrue(source.contains("ModBlocks.SIGNAL_RAIL"));
        assertTrue(source.contains("TrackPathScanner.scanDirected(start, destination"));
        assertTrue(source.contains("TrackPoint incoming = complete.size() >= 2"));
        assertTrue(source.contains("RailConnectionResolver.neighbours"));
        assertTrue(source.contains("RailConnectionResolver.connectedCandidates"));
        assertFalse(source.contains("AbstractMinecart.exits"));
        assertFalse(source.contains("stationTrackCandidates"));
        assertFalse(source.contains("TrackPathScanner.scanAny"));
        assertFalse(source.contains("data.replaceStationStopTracks"));
    }
}
