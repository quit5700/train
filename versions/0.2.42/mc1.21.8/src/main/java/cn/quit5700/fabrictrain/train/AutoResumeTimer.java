package cn.quit5700.fabrictrain.train;

public final class AutoResumeTimer {
    public static final int DEFAULT_DELAY_TICKS = 30 * 20;
    private int remainingTicks;

    public void onStopOrInteraction() {
        remainingTicks = DEFAULT_DELAY_TICKS;
    }

    public boolean tickAndShouldResume() {
        if (remainingTicks <= 0) {
            return false;
        }
        remainingTicks--;
        return remainingTicks == 0;
    }

    public int remainingTicks() {
        return remainingTicks;
    }
}
