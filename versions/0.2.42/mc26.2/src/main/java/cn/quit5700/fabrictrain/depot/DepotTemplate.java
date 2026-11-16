package cn.quit5700.fabrictrain.depot;

import cn.quit5700.fabrictrain.train.CarType;
import cn.quit5700.fabrictrain.train.ConsistRules;

import java.util.ArrayList;
import java.util.List;

public record DepotTemplate(List<CarType> cars, List<Long> destinations) {
    public static final int MAX_ENCODED_LENGTH = 2200;

    public DepotTemplate(List<CarType> cars) {
        this(cars, List.of());
    }

    public long destination(int position) {
        return position < destinations.size() ? destinations.get(position) : 0L;
    }

    public DepotTemplate filterDestinations(java.util.function.LongPredicate allowed) {
        return new DepotTemplate(cars, destinations.stream().map(id -> allowed.test(id) ? id : 0L).toList());
    }

    public DepotTemplate {
        cars = List.copyOf(cars);
        destinations = List.copyOf(destinations.subList(0, Math.min(destinations.size(), cars.size() + 1)));
        if (cars.size() > ConsistRules.ABSOLUTE_MAX_CARS) {
            throw new IllegalArgumentException("Depot template must contain 0 to 100 cars");
        }
    }

    public static DepotTemplate defaultTemplate() {
        return new DepotTemplate(List.of());
    }

    public String encode() {
        StringBuilder encoded = new StringBuilder(cars.size());
        cars.forEach(type -> encoded.append(type == CarType.CARGO ? 'C' : 'P'));
        if (destinations.stream().anyMatch(id -> id != 0L)) {
            encoded.append('|');
            for (int i = 0; i < destinations.size(); i++) {
                if (i > 0) encoded.append(',');
                encoded.append(Math.max(0L, destinations.get(i)));
            }
        }
        return encoded.toString();
    }

    public static DepotTemplate decode(String encoded) {
        int divider = encoded.indexOf('|');
        String formation = divider < 0 ? encoded : encoded.substring(0, divider);
        ArrayList<CarType> cars = new ArrayList<>();
        for (int i = 0; i < formation.length() && cars.size() < ConsistRules.ABSOLUTE_MAX_CARS; i++) {
            if (formation.charAt(i) == 'P') cars.add(CarType.PASSENGER);
            if (formation.charAt(i) == 'C') cars.add(CarType.CARGO);
        }
        ArrayList<Long> destinations = new ArrayList<>();
        if (divider >= 0) {
            String[] ids = encoded.substring(divider + 1).split(",", cars.size() + 2);
            for (int i = 0; i < ids.length && i <= cars.size(); i++) {
                try {
                    destinations.add(Math.max(0L, Long.parseLong(ids[i])));
                } catch (NumberFormatException invalid) {
                    destinations.add(0L);
                }
            }
        }
        return new DepotTemplate(cars, destinations);
    }
}
