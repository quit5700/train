package cn.quit5700.fabrictrain.route;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class TrackPathScanner {
    private TrackPathScanner() {
    }

    public static PathScanResult scan(
            TrackPoint start,
            TrackPoint destination,
            int maximumVisitedPoints,
            Function<TrackPoint, Collection<TrackPoint>> connectedNeighbours) {
        return scanDirected(start, destination, maximumVisitedPoints, null,
                (previous, current) -> connectedNeighbours.apply(current));
    }

    /**
     * Scans using (previous track, current track) as the search state.  Carrying the
     * previous track across station boundaries prevents a route from leaving a
     * station by immediately returning over the track it just used.
     */
    public static PathScanResult scanDirected(
            TrackPoint start,
            TrackPoint destination,
            int maximumVisitedPoints,
            TrackPoint incomingBeforeStart,
            BiFunction<TrackPoint, TrackPoint, Collection<TrackPoint>> connectedNeighbours) {
        if (maximumVisitedPoints < 1) {
            throw new IllegalArgumentException("Maximum visited points must be positive");
        }
        DirectedState initial = new DirectedState(incomingBeforeStart, start);
        ArrayDeque<DirectedState> queue = new ArrayDeque<>();
        Map<DirectedState, DirectedState> predecessor = new HashMap<>();
        Set<TrackPoint> visitedPoints = new HashSet<>();
        queue.add(initial);
        predecessor.put(initial, initial);
        visitedPoints.add(start);

        while (!queue.isEmpty()) {
            DirectedState state = queue.removeFirst();
            if (state.current().equals(destination)) {
                return new PathScanResult(PathScanResult.Status.FOUND, rebuildDirected(predecessor, initial, state));
            }
            if (visitedPoints.size() >= maximumVisitedPoints) {
                return new PathScanResult(PathScanResult.Status.TOO_LONG, List.of());
            }
            Collection<TrackPoint> neighbours = connectedNeighbours.apply(state.previous(), state.current());
            if (neighbours == null) continue;
            for (TrackPoint neighbour : neighbours) {
                if (neighbour == null || neighbour.equals(state.previous())) continue;
                DirectedState next = new DirectedState(state.current(), neighbour);
                if (!predecessor.containsKey(next)) {
                    predecessor.put(next, state);
                    visitedPoints.add(neighbour);
                    queue.addLast(next);
                }
            }
        }
        return new PathScanResult(PathScanResult.Status.NOT_FOUND, List.of());
    }

    private static List<TrackPoint> rebuildDirected(
            Map<DirectedState, DirectedState> predecessor,
            DirectedState start,
            DirectedState destination) {
        ArrayList<TrackPoint> reversed = new ArrayList<>();
        DirectedState current = destination;
        while (!current.equals(start)) {
            reversed.add(current.current());
            current = predecessor.get(current);
        }
        reversed.add(start.current());
        Collections.reverse(reversed);
        return reversed;
    }

    private record DirectedState(TrackPoint previous, TrackPoint current) {}
}
