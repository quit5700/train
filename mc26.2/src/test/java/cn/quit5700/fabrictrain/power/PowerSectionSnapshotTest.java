package cn.quit5700.fabrictrain.power;

import cn.quit5700.fabrictrain.route.TrackPoint;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PowerSectionSnapshotTest {
    @Test
    void dividesLongRoutesIntoSixtyFourTrackSections() {
        List<TrackPoint> path = path(130);
        PowerSectionSnapshot snapshot = PowerSectionSnapshot.create(path, 64,
                ignored -> PowerSectionStatus.POWERED);
        assertEquals(3, snapshot.sectionCount());
        assertTrue(snapshot.allPowered());
    }

    @Test
    void trainChecksCurrentAndNextSectionBeforeEntering() {
        List<TrackPoint> path = path(130);
        PowerSectionSnapshot snapshot = PowerSectionSnapshot.create(path, 64,
                point -> point.x() < 64 ? PowerSectionStatus.POWERED : PowerSectionStatus.UNPOWERED);
        assertFalse(snapshot.canProceedFrom(63));
        assertFalse(snapshot.allPowered());
    }

    @Test
    void unknownUnloadedSectionIsNotMistakenForExplicitPowerLoss() {
        PowerSectionSnapshot snapshot = PowerSectionSnapshot.create(path(70), 64,
                point -> point.x() < 64 ? PowerSectionStatus.POWERED : PowerSectionStatus.UNKNOWN);
        assertEquals(PowerSectionStatus.UNKNOWN, snapshot.sectionStatus(1));
        assertFalse(snapshot.canProceedFrom(63));
    }

    @Test
    void updatingOneRailOnlyChangesItsContainingSection() {
        PowerSectionSnapshot snapshot = PowerSectionSnapshot.create(path(130), 64,
                ignored -> PowerSectionStatus.POWERED).withPointState(70, PowerSectionStatus.UNPOWERED);
        assertEquals(PowerSectionStatus.POWERED, snapshot.sectionStatus(0));
        assertEquals(PowerSectionStatus.UNPOWERED, snapshot.sectionStatus(1));
        assertEquals(PowerSectionStatus.POWERED, snapshot.sectionStatus(2));
    }

    private static List<TrackPoint> path(int size) {
        ArrayList<TrackPoint> result = new ArrayList<>();
        for (int index = 0; index < size; index++) result.add(new TrackPoint(index, 64, 0));
        return result;
    }
}
