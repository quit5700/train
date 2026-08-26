package cn.quit5700.fabrictrain.data;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TrainManifestTest {
    @Test void deletingOneMemberKeepsTrainAndDeletingLastMakesItEmpty() {
        UUID locomotive = UUID.randomUUID();
        UUID passenger = UUID.randomUUID();
        TrainManifest manifest = new TrainManifest(2L, List.of(
                new TrainMember(locomotive, TrainVehicleKind.LOCOMOTIVE, 1),
                new TrainMember(passenger, TrainVehicleKind.PASSENGER, 2)));

        TrainManifest remaining = manifest.without(passenger);
        assertEquals(1, remaining.totalVehicles());
        assertTrue(remaining.contains(locomotive));
        assertEquals(0, remaining.without(locomotive).totalVehicles());
    }

    @Test void duplicateAndGappedSequencesAreNormalizedWithoutChangingEntityIdentity() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        TrainManifest manifest = new TrainManifest(7L, List.of(
                new TrainMember(second, TrainVehicleKind.CARGO, 20),
                new TrainMember(first, TrainVehicleKind.LOCOMOTIVE, 4)));

        assertEquals(List.of(first, second), manifest.members().stream().map(TrainMember::entityId).toList());
        assertEquals(List.of(1, 2), manifest.members().stream().map(TrainMember::sequence).toList());
        assertEquals(1, manifest.locomotives());
        assertEquals(1, manifest.cargoCars());
        assertEquals(7L, manifest.originDepotNumber());
    }
}
