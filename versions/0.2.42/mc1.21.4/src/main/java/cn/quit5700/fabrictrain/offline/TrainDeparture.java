package cn.quit5700.fabrictrain.offline;

import cn.quit5700.fabrictrain.data.LineStatus;
import cn.quit5700.fabrictrain.data.RailwayLine;
import cn.quit5700.fabrictrain.data.StopTrack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** A dispatched train keeps its verified route even when the next dispatch is edited. */
record TrainDeparture(String signature, List<InfrastructureData.Pos> generators, long depotId,
                      List<Long> nodes, Map<Integer, Integer> directions, Map<Integer, StopTrack> stops) {
    private static final Pattern STOP = Pattern.compile("(\\d+)=StopTrack\\[x=(-?\\d+), y=(-?\\d+), z=(-?\\d+)\\]");
    private static final Codec<TrainDeparture> SIGNATURE = Codec.STRING.comapFlatMap(
            value -> {
                var departure = from(value, List.of());
                return departure == null ? DataResult.error(() -> "Invalid train departure route")
                        : DataResult.success(departure);
            }, TrainDeparture::signature);
    static final Codec<TrainDeparture> CODEC = RecordCodecBuilder.create(i -> i.group(
            SIGNATURE.fieldOf("route").forGetter(d -> d),
            InfrastructureData.Pos.CODEC.listOf().fieldOf("generators").forGetter(TrainDeparture::generators)
    ).apply(i, TrainDeparture::withGenerators));

    TrainDeparture {
        generators = List.copyOf(generators);
        nodes = List.copyOf(nodes);
        directions = Map.copyOf(directions);
        stops = Map.copyOf(stops);
    }

    TrainDeparture withGenerators(List<InfrastructureData.Pos> sources) {
        return new TrainDeparture(signature, sources, depotId, nodes, directions, stops);
    }

    static TrainDeparture from(String signature, List<InfrastructureData.Pos> sources) {
        String[] fields = signature.split("\\|", -1);
        if (fields.length != 4 || !fields[1].startsWith("[") || !fields[1].endsWith("]")
                || !fields[2].startsWith("{") || !fields[2].endsWith("}")
                || !fields[3].startsWith("{") || !fields[3].endsWith("}")) return null;
        try {
            long depotId = Long.parseLong(fields[0]);
            var nodes = new ArrayList<Long>();
            for (String node : interior(fields[1]).split(", ")) nodes.add(Long.parseLong(node));
            var directions = new TreeMap<Integer, Integer>();
            if (!interior(fields[2]).isEmpty()) {
                for (String entry : interior(fields[2]).split(", ")) {
                    String[] pair = entry.split("=", -1);
                    if (pair.length != 2) return null;
                    directions.put(Integer.parseInt(pair[0]), Integer.parseInt(pair[1]));
                }
            }
            var stops = new TreeMap<Integer, StopTrack>();
            var matcher = STOP.matcher(interior(fields[3]));
            while (matcher.find()) {
                stops.put(Integer.parseInt(matcher.group(1)), new StopTrack(Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4))));
            }
            // Legacy depot plans store the canonical route as text. Reject partial or malformed reads.
            if (depotId <= 0 || !signature.equals(depotId + "|" + nodes + "|" + directions + "|" + stops)) return null;
            return new TrainDeparture(signature, sources, depotId, nodes, directions, stops);
        } catch (NumberFormatException invalid) {
            return null;
        }
    }

    private static String interior(String value) {
        return value.substring(1, value.length() - 1);
    }

    boolean matches(RailwayLine line) {
        return depotId == line.depotId() && nodes.equals(line.nodeIds())
                && directions.equals(line.switchDirections()) && stops.equals(line.stationStopTracks());
    }

    RailwayLine operatingLine(RailwayLine line) {
        LineStatus status = line.status() == LineStatus.NEEDS_VALIDATION ? LineStatus.RUNNING : line.status();
        return new RailwayLine(line.id(), line.name(), depotId, nodes, line.departureIntervalSeconds(),
                line.speedLimitKmh(), line.maximumTrains(), status, directions, stops);
    }
}
