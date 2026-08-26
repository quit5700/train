package cn.quit5700.fabrictrain.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ResponsiveScreenUsageContractTest {
    @Test void stationAndDepotPinSaveToResponsiveFooter() throws Exception {
        String station = Files.readString(Path.of("src/client/java/cn/quit5700/fabrictrain/client/DeviceSettingsScreen.java"));
        String depot = Files.readString(Path.of("src/client/java/cn/quit5700/fabrictrain/client/DepotSettingsScreen.java"));
        assertTrue(station.contains("layout.footerY(), 70, 20"));
        assertFalse(station.contains("manual_unload_on"));
        assertFalse(station.contains("manual_unload_off"));
        assertTrue(depot.contains("screen.fabric_train_simulator.depot.save"));
        assertTrue(depot.contains("layout.footerY()"));
        assertTrue(station.contains("mouseScrolled"));
        assertTrue(depot.contains("mouseScrolled"));
    }

    @Test void everyCustomSettingsScreenUsesResponsiveLayout() throws Exception {
        for (String name : java.util.List.of("CargoSettingsScreen", "DepotSettingsScreen", "DeviceSettingsScreen",
                "FormationScreen", "LineSettingsScreen", "RailwayManagerScreen", "TrainClassScreen")) {
            String source = Files.readString(Path.of("src/client/java/cn/quit5700/fabrictrain/client/" + name + ".java"));
            assertTrue(source.contains("ResponsiveScreenLayout"), name + " must use the shared responsive layout");
        }
    }

    @Test void depotAlwaysRendersOrderedCarriageTypes() throws Exception {
        String depot = Files.readString(Path.of("src/client/java/cn/quit5700/fabrictrain/client/DepotSettingsScreen.java"));
        assertTrue(depot.contains("carriage_sequence"));
        assertTrue(depot.contains("cars.get(i)"));
    }
}
