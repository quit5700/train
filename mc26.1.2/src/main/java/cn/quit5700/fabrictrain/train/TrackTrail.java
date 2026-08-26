package cn.quit5700.fabrictrain.train;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class TrackTrail {
    private static final double MIN_SAMPLE_DISTANCE = 0.08D;
    private final Deque<Point> points = new ArrayDeque<>();
    private final double maximumLength;

    public TrackTrail(double maximumLength) {
        this.maximumLength = maximumLength;
    }

    public void record(double x, double y, double z) {
        Point next = new Point(x, y, z);
        Point first = points.peekFirst();
        if (first != null && first.distance(next) < MIN_SAMPLE_DISTANCE) return;
        points.addFirst(next);
        trim();
    }

    public Point pointBehind(double distance) {
        if (points.isEmpty()) return null;
        if (distance <= 0) return points.peekFirst();
        List<Point> copy = new ArrayList<>(points);
        double walked = 0;
        for (int i = 1; i < copy.size(); i++) {
            Point from = copy.get(i - 1);
            Point to = copy.get(i);
            double segment = from.distance(to);
            if (walked + segment >= distance) {
                double ratio = (distance - walked) / segment;
                return from.interpolate(to, ratio);
            }
            walked += segment;
        }
        return null;
    }

    private void trim() {
        double length = 0;
        Point previous = null;
        for (Point point : new ArrayList<>(points)) {
            if (previous != null) length += previous.distance(point);
            if (length > maximumLength) {
                while (points.peekLast() != point) points.removeLast();
                points.removeLast();
                return;
            }
            previous = point;
        }
    }

    public record Point(double x, double y, double z) {
        double distance(Point other) {
            double dx = x - other.x, dy = y - other.y, dz = z - other.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        Point interpolate(Point other, double ratio) {
            return new Point(x + (other.x - x) * ratio, y + (other.y - y) * ratio, z + (other.z - z) * ratio);
        }
    }
}
