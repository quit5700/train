package cn.quit5700.fabrictrain.freight;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class FreightBrowseRulesTest {
    @Test void tabsAndItemsResolveWithoutAnyRenderState() {
        assertEquals(1, FreightBrowseRules.cell(61 + 12, 61, 59, 4));
        assertEquals(3, FreightBrowseRules.cell(3 * 61 + 58.99, 61, 59, 4));
        assertEquals(-1, FreightBrowseRules.cell(59, 61, 59, 4));
        assertEquals(2, FreightBrowseRules.cell(2 * 18 + 8, 18, 17, 9));
        assertEquals(-1, FreightBrowseRules.cell(17, 18, 17, 9));
        assertEquals(-1, FreightBrowseRules.cell(9 * 18, 18, 17, 9));
    }

    @Test void categoryAndExistingSelectionAreIndependentOfPhysicalStock() {
        Set<String> selected = Set.of("minecraft:iron_ingot", "minecraft:iron_pickaxe");
        assertTrue(FreightBrowseRules.visible("minecraft:iron_ingot", false, "ore", true, selected));
        assertFalse(FreightBrowseRules.visible("minecraft:iron_pickaxe", false, "ore", true, selected));
        assertFalse(FreightBrowseRules.visible("minecraft:diamond", false, "ore", true, selected));
        assertTrue(FreightBrowseRules.visible("minecraft:diamond", false, "ore", false, selected));
        assertEquals("ore", FreightBrowseRules.category("minecraft:deepslate_redstone_ore", true));
        assertEquals("transport", FreightBrowseRules.category("fabric_train_simulator:signal_rail", true));
        assertEquals("build", FreightBrowseRules.category("minecraft:oak_planks", true));
    }
}
