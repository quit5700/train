package cn.quit5700.fabrictrain.depot;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

final class DepotDispatchResultTest {
    @Test
    void onlySuccessIsSuccessfulAndEveryOutcomeHasSpecificTranslationKey() {
        for (DepotDispatchResult result : DepotDispatchResult.values()) {
            assertEquals(result == DepotDispatchResult.SUCCESS, result.succeeded());
            assertTrue(result.translationKey().startsWith("message.fabric_train_simulator.depot."));
        }
        assertEquals(DepotDispatchResult.values().length,
                Arrays.stream(DepotDispatchResult.values()).map(DepotDispatchResult::translationKey).distinct().count());
    }
}
