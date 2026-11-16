package cn.quit5700.fabrictrain.train;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class TrainSpeedPresentation {
    public enum InputError { INVALID_NUMBER, OUT_OF_RANGE }

    private TrainSpeedPresentation() {}

    public static String format(double kmh) {
        return BigDecimal.valueOf(kmh).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    public static InputError inputError(String input) {
        try {
            double speed = Double.parseDouble(input);
            if (!Double.isFinite(speed)) return InputError.INVALID_NUMBER;
            return speed < 0 || speed > SpeedRules.MAX_CONFIGURED_LIMIT_KMH ? InputError.OUT_OF_RANGE : null;
        } catch (NumberFormatException e) {
            return InputError.INVALID_NUMBER;
        }
    }

    public static double changedValue(String input, double initial) {
        double speed = Double.parseDouble(input);
        return speed == initial || input.equals(format(initial)) ? Double.NaN : speed;
    }

    public static double measuredSpeed(double blocksMovedInOneTick) {
        return blocksMovedInOneTick / SpeedRules.blocksPerTick(1.0D);
    }
}
