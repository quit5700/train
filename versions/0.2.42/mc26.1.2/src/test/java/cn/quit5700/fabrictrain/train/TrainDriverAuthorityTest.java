package cn.quit5700.fabrictrain.train;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrainDriverAuthorityTest {
    @Test
    void passengerInSecondOrThirdCarCanControlWhenClosestToLeader() {
        UUID secondCarPassenger = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID thirdCarPassenger = UUID.fromString("00000000-0000-0000-0000-000000000003");

        UUID driver = TrainDriverAuthority.closest(List.of(
                new TrainDriverAuthority.Rider(secondCarPassenger, 1.1D),
                new TrainDriverAuthority.Rider(thirdCarPassenger, 4.2D)));

        assertEquals(secondCarPassenger, driver);
    }
}
