package cn.quit5700.fabrictrain.depot;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class DepotDispatchBindingContractTest {
    @Test
    void depotSettingsUseSavedDataBindingAndOfferEveryLine() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/network/DepotNetworking.java"), StandardCharsets.UTF_8);

        assertTrue(source.contains("data.bindDispatchDepot(device.id(), payload.lineId())"));
        assertTrue(source.contains("data.dispatchLineIdForDepot(depotId)"));
        assertFalse(source.contains("line.nodeIds().getFirst() == deviceIdAt"),
                "车辆段线路选择不得再按线路第一个节点过滤");
    }

    @Test
    void everySelectedLineDispatchValidatesThePoweredAccessPath() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/depot/DepotDispatchService.java"), StandardCharsets.UTF_8);

        assertTrue(source.contains("WorldLineValidator.validateDispatchAccess"));
        assertTrue(source.contains("DispatchAccessStatus.UNPOWERED"));
        assertTrue(source.contains("access.path().get(1)"),
                "发车方向必须来自通往线路第一个节点的已验证路径");
    }
}
