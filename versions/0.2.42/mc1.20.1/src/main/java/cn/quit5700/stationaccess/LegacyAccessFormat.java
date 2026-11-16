package cn.quit5700.stationaccess;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.nbt.*;

/** Converts only the SavedData envelope before legacy Minecraft reads it. */
final class LegacyAccessFormat {
    private LegacyAccessFormat() {}
    static void prepare(Path world, Consumer<String> validate) {
        Path file=world.resolve("data/station_access_access.dat");
        if(!Files.isRegularFile(file)) return;
        Path temp=null;
        try {
            byte[] original=Files.readAllBytes(file);
            CompoundTag root=read(original);
            Tag payload=root.get("data");
            if(payload instanceof CompoundTag) return;
            if(!(payload instanceof ListTag parts) || parts.getElementType()!=Tag.TAG_STRING)
                throw new IOException("Unrecognized station access envelope");
            StringBuilder json=new StringBuilder();
            for(int i=0;i<parts.size();i++) json.append(parts.getString(i));
            validate.accept(json.toString());
            CompoundTag legacy=new CompoundTag();
            legacy.put("parts",parts.copy());
            root.put("data",legacy);
            temp=Files.createTempFile(file.getParent(),"station-access-",".tmp");
            try(OutputStream out=Files.newOutputStream(temp)){NbtIo.writeCompressed(root,out);}
            if(!root.equals(read(Files.readAllBytes(temp)))) throw new IOException("Station access verification failed");
            if(!Arrays.equals(original,Files.readAllBytes(file))) throw new IOException("Station access changed during migration");
            try {Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException unsupported){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
        } catch(IOException failure) {
            throw new IllegalStateException("Cannot migrate station access without discarding data",failure);
        } finally {
            if(temp!=null)try {Files.deleteIfExists(temp);} catch(IOException ignored) {}
        }
    }
    private static CompoundTag read(byte[] bytes) throws IOException {
        try(InputStream input=new ByteArrayInputStream(bytes)) {
            return NbtIo.readCompressed(input);
        }
    }
}
