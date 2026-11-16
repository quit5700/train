package cn.quit5700.fabrictrain.power;

import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;

public final class GeneratorFuelState {
    private long activeUntilGameTime;
    private long legacyRemainingTicks;
    private int reservedFuelSlot;

    private GeneratorFuelState(long activeUntilGameTime, long legacyRemainingTicks, int reservedFuelSlot) {
        this.activeUntilGameTime = Math.max(0L, activeUntilGameTime);
        this.legacyRemainingTicks = Math.max(0L, legacyRemainingTicks);
        this.reservedFuelSlot = reservedFuelSlot;
    }

    public static GeneratorFuelState empty() {
        return new GeneratorFuelState(0L, 0L, -1);
    }

    public static GeneratorFuelState restore(long activeUntilGameTime, long legacyRemainingTicks,
                                             int reservedFuelSlot) {
        return new GeneratorFuelState(activeUntilGameTime,
                activeUntilGameTime > 0L ? 0L : legacyRemainingTicks,
                activeUntilGameTime > 0L ? reservedFuelSlot : -1);
    }

    public boolean tryActivate(long currentGameTime, long durationTicks, int fuelSlot,
                               BooleanSupplier commitActivation) {
        if (durationTicks <= 0L || isActive(currentGameTime)) return false;
        long previousUntil = activeUntilGameTime;
        int previousSlot = reservedFuelSlot;
        activeUntilGameTime = saturatingAdd(Math.max(0L, currentGameTime), durationTicks);
        reservedFuelSlot = fuelSlot;
        try {
            if (commitActivation.getAsBoolean()) {
                legacyRemainingTicks = 0L;
                return true;
            }
            activeUntilGameTime = previousUntil;
            reservedFuelSlot = previousSlot;
            return false;
        } catch (RuntimeException | Error failure) {
            activeUntilGameTime = previousUntil;
            reservedFuelSlot = previousSlot;
            throw failure;
        }
    }

    public boolean settleExpiredFuel(long currentGameTime, IntPredicate consumeFuel) {
        if (activeUntilGameTime <= 0L || currentGameTime < activeUntilGameTime) return false;
        if (reservedFuelSlot >= 0 && !consumeFuel.test(reservedFuelSlot)) return false;
        activeUntilGameTime = 0L;
        reservedFuelSlot = -1;
        return true;
    }

    public boolean migrateLegacyRemaining(long currentGameTime) {
        if (activeUntilGameTime > 0L || legacyRemainingTicks <= 0L) return false;
        activeUntilGameTime = saturatingAdd(Math.max(0L, currentGameTime), legacyRemainingTicks);
        legacyRemainingTicks = 0L;
        reservedFuelSlot = -1;
        return true;
    }

    public boolean isActive(long currentGameTime) {
        return activeUntilGameTime > Math.max(0L, currentGameTime);
    }

    public long remainingTicks(long currentGameTime) {
        if (activeUntilGameTime > 0L) return Math.max(0L, activeUntilGameTime - Math.max(0L, currentGameTime));
        return legacyRemainingTicks;
    }

    public long activeUntilGameTime() {
        return activeUntilGameTime;
    }

    public int reservedFuelSlot() {
        return reservedFuelSlot;
    }

    private static long saturatingAdd(long left, long right) {
        if (right > Long.MAX_VALUE - left) return Long.MAX_VALUE;
        return left + right;
    }
}
