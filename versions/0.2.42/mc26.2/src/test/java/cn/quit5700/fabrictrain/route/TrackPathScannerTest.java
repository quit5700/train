package cn.quit5700.fabrictrain.route;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrackPathScannerTest {
    @Test
    void findsConnectedTrackPathInOrder() {
        TrackPoint a = new TrackPoint(0, 64, 0);
        TrackPoint b = new TrackPoint(1, 64, 0);
        TrackPoint c = new TrackPoint(2, 64, 0);
        Map<TrackPoint, List<TrackPoint>> graph = Map.of(a, List.of(b), b, List.of(a, c), c, List.of(b));

        PathScanResult result = TrackPathScanner.scan(a, c, 100, point -> graph.getOrDefault(point, List.of()));

        assertEquals(PathScanResult.Status.FOUND, result.status());
        assertEquals(List.of(a, b, c), result.path());
    }

    @Test
    void reportsBrokenTrack() {
        TrackPoint a = new TrackPoint(0, 64, 0);
        TrackPoint c = new TrackPoint(2, 64, 0);
        PathScanResult result = TrackPathScanner.scan(a, c, 100, point -> List.of());
        assertEquals(PathScanResult.Status.NOT_FOUND, result.status());
    }

    @Test
    void nextSegmentInheritsIncomingTrackAndCannotImmediatelyReverseAtStation() {
        TrackPoint incoming = new TrackPoint(0, 64, 0);
        TrackPoint station = new TrackPoint(1, 64, 0);
        TrackPoint forward = new TrackPoint(2, 64, 0);
        TrackPoint detour = new TrackPoint(2, 65, 1);
        TrackPoint destination = new TrackPoint(0, 64, 1);
        Map<TrackPoint, List<TrackPoint>> graph = Map.of(
                station, List.of(incoming, forward),
                incoming, List.of(station, destination),
                forward, List.of(station, detour),
                detour, List.of(forward, destination),
                destination, List.of(incoming, detour));

        PathScanResult result = TrackPathScanner.scanDirected(station, destination, 100, incoming,
                (previous, current) -> graph.getOrDefault(current, List.of()));

        assertEquals(PathScanResult.Status.FOUND, result.status());
        assertEquals(List.of(station, forward, detour, destination), result.path());
    }

    @Test
    void directedStateCanVisitSameJunctionFromDifferentIncomingTracks() {
        TrackPoint a = new TrackPoint(0, 64, 0);
        TrackPoint junction = new TrackPoint(1, 64, 0);
        TrackPoint branchOne = new TrackPoint(1, 65, 1);
        TrackPoint branchTwo = new TrackPoint(2, 65, 1);
        TrackPoint destination = new TrackPoint(2, 64, 0);
        Map<TrackPoint, List<TrackPoint>> graph = Map.of(
                a, List.of(junction), junction, List.of(a, branchOne, branchTwo, destination),
                branchOne, List.of(junction, branchTwo), branchTwo, List.of(branchOne, junction),
                destination, List.of(junction));
        PathScanResult result = TrackPathScanner.scanDirected(a, destination, 100, null,
                (previous, current) -> current.equals(junction) && previous != null && previous.equals(a)
                        ? List.of(a, branchOne) : graph.getOrDefault(current, List.of()));
        assertEquals(PathScanResult.Status.FOUND, result.status());
        assertEquals(List.of(a, junction, branchOne, branchTwo, junction, destination), result.path());
    }

    @Test
    void twoStationsOnASlopedLoopContinueForwardInsteadOfReversing() {
        TrackPoint beforeA = new TrackPoint(-1, 64, 0);
        TrackPoint stationA = new TrackPoint(0, 64, 0);
        TrackPoint east = new TrackPoint(1, 64, 0);
        TrackPoint stationB = new TrackPoint(2, 64, 0);
        TrackPoint raisedCurve = new TrackPoint(2, 65, 1);
        TrackPoint returnCurve = new TrackPoint(0, 64, 1);
        Map<TrackPoint, List<TrackPoint>> graph = Map.of(
                beforeA, List.of(stationA), stationA, List.of(beforeA, east, returnCurve),
                east, List.of(stationA, stationB), stationB, List.of(east, raisedCurve),
                raisedCurve, List.of(stationB, returnCurve), returnCurve, List.of(raisedCurve, stationA));

        PathScanResult first = TrackPathScanner.scanDirected(stationA, stationB, 100, beforeA,
                (previous, current) -> graph.getOrDefault(current, List.of()));
        PathScanResult second = TrackPathScanner.scanDirected(stationB, stationA, 100, east,
                (previous, current) -> graph.getOrDefault(current, List.of()));

        assertEquals(List.of(stationA, east, stationB), first.path());
        assertEquals(List.of(stationB, raisedCurve, returnCurve, stationA), second.path());
    }
}
