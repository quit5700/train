package cn.quit5700.fabrictrain.train;

final class LogicalSpeedState {
    private double speed;

    double speed() {
        return speed;
    }

    double approach(double target, double acceleration) {
        if (speed < target) speed = Math.min(target, speed + acceleration);
        else if (speed > target) speed = Math.max(target, speed - acceleration);
        if (Math.abs(speed) < 0.0001D) speed = 0;
        return speed;
    }

    void stop() {
        speed = 0;
    }

    void restore(double value) {
        speed = Double.isFinite(value) ? value : 0D;
    }
}
