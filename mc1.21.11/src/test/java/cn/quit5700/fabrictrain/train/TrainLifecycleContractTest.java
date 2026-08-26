package cn.quit5700.fabrictrain.train;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainLifecycleContractTest {
    @Test
    void everyVehicleReportsDestructiveRemovalAndUnifiedRetirementClearsRuntimeState() throws Exception {
        for (String entity : new String[]{"LocomotiveEntity", "PassengerCarEntity", "CargoCarEntity"}) {
            String source = Files.readString(Path.of(
                    "src/main/java/cn/quit5700/fabrictrain/entity/" + entity + ".java"), StandardCharsets.UTF_8);
            assertTrue(source.contains("noteVehicleRemoved"), entity);
        }
        String controller = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/train/TrainControllerManager.java"), StandardCharsets.UTF_8);
        assertTrue(controller.contains("retireTrainResources"));
        assertTrue(controller.contains("data.markTrainForDeletion(trainId)"));
        assertTrue(controller.contains("releaseTrainSwitches(trainId)"));
        assertTrue(controller.contains("reason.shouldDestroy()"));
        assertTrue(controller.contains("TrainPresence.UNLOADED"));
        assertTrue(controller.contains("TrainRegistryService.removeVehicle"));
        assertTrue(controller.contains("MemberRemovalResult.TRAIN_EMPTY"));
    }
}
