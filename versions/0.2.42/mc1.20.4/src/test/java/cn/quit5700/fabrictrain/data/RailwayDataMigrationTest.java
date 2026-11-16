package cn.quit5700.fabrictrain.data;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.io.DataOutputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RailwayDataMigrationTest {
    @TempDir Path temporaryDirectory;

    @Test void copiesNewestLegacyDataToCanonicalPathAndKeepsBackup() throws Exception {
        Path flat = temporaryDirectory.resolve("data/fabric_train_simulator_railway_data.dat");
        Path namespaced = temporaryDirectory.resolve("dimensions/minecraft/overworld/data/fabric_train_simulator/railway_data.dat");
        Files.createDirectories(flat.getParent());
        Files.write(flat, new byte[]{1, 2, 3});
        Files.setLastModifiedTime(flat, FileTime.fromMillis(2_000L));

        RailwayDataMigration.migrate(temporaryDirectory, true);

        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(namespaced));
        assertTrue(Files.exists(flat.resolveSibling(flat.getFileName() + ".pre-0.2.36.bak")));
    }

    @Test void doesNotReplaceCanonicalDataWithAnOlderLegacyFile() throws Exception {
        Path flat = temporaryDirectory.resolve("data/fabric_train_simulator_railway_data.dat");
        Path namespaced = temporaryDirectory.resolve("dimensions/minecraft/overworld/data/fabric_train_simulator/railway_data.dat");
        Files.createDirectories(flat.getParent());
        Files.createDirectories(namespaced.getParent());
        Files.write(flat, new byte[]{1});
        Files.write(namespaced, new byte[]{9});
        Files.setLastModifiedTime(flat, FileTime.fromMillis(1_000L));
        Files.setLastModifiedTime(namespaced, FileTime.fromMillis(2_000L));

        RailwayDataMigration.migrate(temporaryDirectory, true);

        assertArrayEquals(new byte[]{9}, Files.readAllBytes(namespaced));
    }

    @Test void usefulBackupWinsOverNewerEmptyCanonicalSave() throws Exception {
        Path canonical = temporaryDirectory.resolve("data/fabric_train_simulator_railway_data.dat");
        Path backup = canonical.resolveSibling(canonical.getFileName() + ".pre-0.2.36.bak");
        Files.createDirectories(canonical.getParent());
        writeNbt(canonical, 0, 0);
        writeNbt(backup, 4, 1);
        Files.setLastModifiedTime(backup, FileTime.fromMillis(1_000L));
        Files.setLastModifiedTime(canonical, FileTime.fromMillis(2_000L));

        RailwayDataMigration.migrate(temporaryDirectory, false);

        assertTrue(RailwayDataMigration.contentScore(canonical) >= 1_004_000);
        assertTrue(Files.exists(canonical.resolveSibling(canonical.getFileName() + ".recovery.bak")));
    }

    @Test void olderLargerBackupCannotReplaceCurrentValidLineEdit() throws Exception {
        Path canonical=temporaryDirectory.resolve("data/fabric_train_simulator_railway_data.dat");
        Path backup=canonical.resolveSibling(canonical.getFileName()+".recovery.bak");
        Files.createDirectories(canonical.getParent());writeNbt(canonical,12,1);writeNbt(backup,13,4);
        Files.setLastModifiedTime(backup,FileTime.fromMillis(1000));Files.setLastModifiedTime(canonical,FileTime.fromMillis(2000));
        byte[] expected=Files.readAllBytes(canonical);RailwayDataMigration.migrate(temporaryDirectory,false);
        assertArrayEquals(expected,Files.readAllBytes(canonical));
    }
    @Test void explicitEmptyCurrentWorldMustNotResurrectDeletedLines() throws Exception {
        Path canonical=temporaryDirectory.resolve("data/fabric_train_simulator_railway_data.dat");
        Path backup=canonical.resolveSibling(canonical.getFileName()+".recovery.bak");Files.createDirectories(canonical.getParent());
        try(var out=new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(canonical)))){out.writeByte(10);out.writeUTF("");out.writeByte(3);out.writeUTF("SchemaVersion");out.writeInt(1);writeEmptyCompoundList(out,"Devices",0);writeEmptyCompoundList(out,"Lines",0);out.writeByte(0);}
        writeNbt(backup,13,4);byte[] expected=Files.readAllBytes(canonical);RailwayDataMigration.migrate(temporaryDirectory,false);
        assertArrayEquals(expected,Files.readAllBytes(canonical));
    }

    private static void writeNbt(Path path, int devices, int lines) throws Exception {
        try (DataOutputStream output = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(path)))) {
            output.writeByte(10); output.writeUTF("");
            writeEmptyCompoundList(output, "Devices", devices);
            writeEmptyCompoundList(output, "Lines", lines);
            output.writeByte(0);
        }
    }

    private static void writeEmptyCompoundList(DataOutputStream output, String name, int count) throws Exception {
        output.writeByte(9); output.writeUTF(name); output.writeByte(10); output.writeInt(count);
        for (int i = 0; i < count; i++) output.writeByte(0);
    }
}
