package cn.quit5700.fabrictrain.power;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

final class PowerGeneratorFuelActivationTest {
    @Test
    void activationReservesFuelWithoutConsumingItImmediately() {
        GeneratorFuelState state = GeneratorFuelState.empty();
        AtomicInteger fuel = new AtomicInteger(1);

        assertTrue(state.tryActivate(100L, 504_000L, 3, () -> {
            assertEquals(504_100L, state.activeUntilGameTime());
            assertEquals(3, state.reservedFuelSlot());
            return fuel.get() > 0;
        }));

        assertEquals(1, fuel.get());
        assertTrue(state.isActive(100L));
        assertEquals(504_000L, state.remainingTicks(100L));
    }

    @Test
    void reservedFuelIsConsumedExactlyOnceWhenBurnTimeEnds() {
        GeneratorFuelState state = GeneratorFuelState.empty();
        AtomicInteger fuel = new AtomicInteger(1);
        assertTrue(state.tryActivate(100L, 504_000L, 3, () -> true));

        assertFalse(state.settleExpiredFuel(504_099L, slot -> fuel.getAndDecrement() > 0));
        assertEquals(1, fuel.get());
        assertTrue(state.settleExpiredFuel(504_100L, slot -> {
            assertEquals(3, slot);
            return fuel.getAndDecrement() > 0;
        }));

        assertEquals(0, fuel.get());
        assertEquals(-1, state.reservedFuelSlot());
        assertEquals(0L, state.activeUntilGameTime());
        assertFalse(state.settleExpiredFuel(504_101L, slot -> {
            fail("同一份燃料不得重复消耗");
            return true;
        }));
    }

    @Test
    void failedFuelCommitRollsBackWithoutCreatingPower() {
        GeneratorFuelState state = GeneratorFuelState.empty();

        assertFalse(state.tryActivate(500L, 72_000L, 2, () -> false));

        assertFalse(state.isActive(500L));
        assertEquals(0L, state.activeUntilGameTime());
        assertEquals(-1, state.reservedFuelSlot());
    }

    @Test
    void exceptionDuringCommitRollsBackState() {
        GeneratorFuelState state = GeneratorFuelState.empty();

        assertThrows(IllegalStateException.class,
                () -> state.tryActivate(500L, 72_000L, 2,
                        () -> { throw new IllegalStateException("probe"); }));

        assertFalse(state.isActive(500L));
        assertEquals(0L, state.activeUntilGameTime());
        assertEquals(-1, state.reservedFuelSlot());
    }

    @Test
    void absoluteEndTimeSurvivesSaveAndReload() {
        GeneratorFuelState original = GeneratorFuelState.empty();
        assertTrue(original.tryActivate(2_000L, 72_000L, 4, () -> true));

        GeneratorFuelState restored = GeneratorFuelState.restore(
                original.activeUntilGameTime(), 0L, original.reservedFuelSlot());

        assertEquals(74_000L, restored.activeUntilGameTime());
        assertEquals(71_000L, restored.remainingTicks(3_000L));
        assertTrue(restored.isActive(3_000L));
        assertEquals(4, restored.reservedFuelSlot());
    }

    @Test
    void legacyRemainingTicksMigrateOnFirstServerTick() {
        GeneratorFuelState restored = GeneratorFuelState.restore(0L, 72_000L, -1);

        assertTrue(restored.migrateLegacyRemaining(5_000L));

        assertEquals(77_000L, restored.activeUntilGameTime());
        assertEquals(72_000L, restored.remainingTicks(5_000L));
    }
}
