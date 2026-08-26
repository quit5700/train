package cn.quit5700.fabrictrain.depot;

import cn.quit5700.fabrictrain.train.CarType;
import cn.quit5700.fabrictrain.train.ConsistRules;

import java.util.ArrayList;
import java.util.List;

public record DepotTemplate(List<CarType> cars) {
    public DepotTemplate {
        cars = List.copyOf(cars);
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
        return encoded.toString();
    }

    public static DepotTemplate decode(String encoded) {
        ArrayList<CarType> cars = new ArrayList<>();
        for (int i = 0; i < encoded.length() && cars.size() < ConsistRules.ABSOLUTE_MAX_CARS; i++) {
            if (encoded.charAt(i) == 'P') cars.add(CarType.PASSENGER);
            if (encoded.charAt(i) == 'C') cars.add(CarType.CARGO);
        }
        return new DepotTemplate(cars);
    }
}
