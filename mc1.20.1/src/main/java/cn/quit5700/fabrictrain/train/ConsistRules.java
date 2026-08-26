package cn.quit5700.fabrictrain.train;

import java.util.List;

public final class ConsistRules {
    public static final int DEFAULT_MAX_CARS = 8;
    public static final int ABSOLUTE_MAX_CARS = 100;

    private ConsistRules() {
    }

    public static TrainClass normalizeClass(List<CarType> cars, TrainClass requestedClass) {
        if (cars.contains(CarType.CARGO)) {
            return TrainClass.CARGO;
        }
        return requestedClass;
    }

    public static boolean validCarCount(int carCount, int configuredMaximum) {
        int effectiveMaximum = cn.quit5700.fabrictrain.compat.MathCompat.clamp(configuredMaximum, 1, ABSOLUTE_MAX_CARS);
        return carCount >= 1 && carCount <= effectiveMaximum;
    }
}

