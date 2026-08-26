package cn.quit5700.fabrictrain.power;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class PowerGeneratorServerTickContractTest {
    @Test
    void inventoryInsertionDoesNotConsumeFuelOnTheClientOrBeforeServerTick() throws Exception {
        String source = Files.readString(Path.of("src/main/java/cn/quit5700/fabrictrain/block/entity/PowerGeneratorBlockEntity.java"),
                StandardCharsets.UTF_8);

        assertFalse(source.contains("@Override public void setItem"));
        assertTrue(source.contains("fuelState.tryActivate"));
        assertTrue(source.contains("FuelUntilGameTime"));
        int expiration = source.indexOf("fuelState.settleExpiredFuel");
        int transaction = source.indexOf("fuelState.tryActivate");
        int activationEnd = source.indexOf("private boolean consumeReservedFuel", transaction);
        String activationSource = source.substring(transaction, activationEnd);
        assertTrue(expiration >= 0 && transaction > expiration,
                "服务器必须先结算到期燃料，再尝试启动下一份燃料");
        assertFalse(activationSource.contains("shrink(1)"),
                "燃料启动时必须保留在槽位中，不得瞬间消失");
        assertTrue(source.contains("isFuelSlotLocked"));
        assertTrue(source.contains("ReservedFuelSlot"));
    }

    @Test
    void generatorMenuUsesTheBlockEntityInventoryInsteadOfATemporaryChest() throws Exception {
        String source = Files.readString(Path.of("src/main/java/cn/quit5700/fabrictrain/block/entity/PowerGeneratorBlockEntity.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("new ChestMenu(MenuType.GENERIC_9x1, syncId, inventory, this, 1)"),
                "发电机菜单必须把方块实体 this 作为 9 格容器传给 ChestMenu");
        assertFalse(source.contains("ChestMenu.oneRow(syncId, inventory)"),
                "oneRow 两参数重载会创建临时 SimpleContainer，关窗后燃料会消失");
    }
}
