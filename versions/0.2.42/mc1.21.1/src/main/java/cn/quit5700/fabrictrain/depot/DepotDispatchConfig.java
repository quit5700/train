package cn.quit5700.fabrictrain.depot;

import cn.quit5700.fabrictrain.block.entity.TrainDepotBlockEntity;
import cn.quit5700.fabrictrain.train.TrainClass;

public record DepotDispatchConfig(DepotTemplate template, TrainClass trainClass, int intervalSeconds,
                                  int maximumTrains, double cargoSpeedKmh, double passengerSpeedKmh,
                                  double expressSpeedKmh) {
    public static DepotDispatchConfig saved(TrainDepotBlockEntity depot) {
        return new DepotDispatchConfig(depot.template(), depot.trainClass(), depot.dispatchIntervalSeconds(),
                depot.maximumTrains(), depot.speedLimitKmh(TrainClass.CARGO),
                depot.speedLimitKmh(TrainClass.PASSENGER), depot.speedLimitKmh(TrainClass.EXPRESS));
    }

    public double speedLimitKmh(TrainClass value) {
        return switch (value) {
            case CARGO -> cargoSpeedKmh;
            case PASSENGER -> passengerSpeedKmh;
            case EXPRESS -> expressSpeedKmh;
        };
    }

    public TrainClass effectiveClass() {
        return template.cars().contains(cn.quit5700.fabrictrain.train.CarType.CARGO)
                ? TrainClass.CARGO : trainClass;
    }
}
