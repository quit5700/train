package cn.quit5700.fabrictrain.train;

public final class SpeedRules {
    public static final double FREIGHT_LIMIT_KMH = 28.8D;
    public static final double PASSENGER_LIMIT_KMH = 120.6D;
    public static final double EXPRESS_LIMIT_KMH = PASSENGER_LIMIT_KMH * 1.5D;
    public static final double MAX_CONFIGURED_LIMIT_KMH = 1000.0D;
    private SpeedRules() {
    }

    public static double actualSpeed(double lineLimit, double classLimit) {
        if (!Double.isFinite(lineLimit) || !Double.isFinite(classLimit) || lineLimit < 0 || classLimit < 0) {
            throw new IllegalArgumentException("Speed limits must be finite and non-negative");
        }
        return Math.min(lineLimit, classLimit);
    }

    public static double actualSpeed(double lineLimit, double classLimit, double configuredLimit) {
        double configured = configuredLimit > 0D ? configuredLimit : classLimit;
        return Math.min(actualSpeed(lineLimit, classLimit), configured);
    }

    public static double sanitizeConfiguredLimit(double value, double fallback) {
        if (!Double.isFinite(value) || value <= 0D) return fallback;
        return cn.quit5700.fabrictrain.compat.MathCompat.clamp(value, 0.1D, MAX_CONFIGURED_LIMIT_KMH);
    }

    public static double expressLimit(double passengerLimit) {
        if (!Double.isFinite(passengerLimit) || passengerLimit < 0) {
            throw new IllegalArgumentException("Passenger limit must be finite and non-negative");
        }
        return passengerLimit * 1.5D;
    }

    public static double classLimit(TrainClass trainClass) {
        return switch (trainClass) {
            case CARGO -> FREIGHT_LIMIT_KMH;
            case PASSENGER -> PASSENGER_LIMIT_KMH;
            case EXPRESS -> EXPRESS_LIMIT_KMH;
        };
    }

    public static double blocksPerTick(double speedKmh) {
        if (!Double.isFinite(speedKmh) || speedKmh < 0) throw new IllegalArgumentException("Speed must be finite and non-negative");
        return speedKmh / 72.0D;
    }
}

