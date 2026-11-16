package cn.quit5700.stationaccess;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LegacyAccessFormatTest {
    @TempDir Path temp;
    private Path file(Path world){return world.resolve("data/station_access_access.dat");}
    private void write(Path world,CompoundTag root)throws IOException {
        Files.createDirectories(file(world).getParent());
        try(OutputStream out=Files.newOutputStream(file(world))){NbtIo.writeCompressed(root,out);}
    }
    private CompoundTag read(Path world)throws IOException {
        try(InputStream in=Files.newInputStream(file(world))){return NbtIo.readCompressed(in,NbtAccounter.unlimitedHeap());}
    }
    @Test void modernEnvelopePreservesAllPartsBalancesAndUnrelatedTags()throws Exception {
        Path world=temp.resolve("测试 宝箱 中文路径");
        String first="{\"areas\":[{\"name\":\"站点甲\"}],",second="\"wallets\":{\"balances\":{\"票甲\":138}}}";
        ListTag parts=new ListTag();parts.add(StringTag.valueOf(first));parts.add(StringTag.valueOf(second));
        CompoundTag root=new CompoundTag();root.put("data",parts);root.putInt("DataVersion",3700);root.putLong("keep",987654321L);write(world,root);
        AtomicReference<String> json=new AtomicReference<>();LegacyAccessFormat.prepare(world,json::set);
        assertEquals(first+second,json.get());CompoundTag actual=read(world);
        assertEquals(parts,actual.getCompound("data").getList("parts",Tag.TAG_STRING));
        actual.put("data",parts);assertEquals(root,actual);
        byte[] once=Files.readAllBytes(file(world));LegacyAccessFormat.prepare(world,s->fail("Legacy data must not be reparsed"));assertArrayEquals(once,Files.readAllBytes(file(world)));
    }
    @Test void invalidPayloadCannotBeReplacedWithEmptyData()throws Exception {
        Path world=temp.resolve("坏数据");CompoundTag root=new CompoundTag();ListTag parts=new ListTag();parts.add(StringTag.valueOf("invalid"));root.put("data",parts);write(world,root);byte[] before=Files.readAllBytes(file(world));
        assertThrows(IllegalArgumentException.class,()->LegacyAccessFormat.prepare(world,s->{throw new IllegalArgumentException("invalid JSON");}));
        assertArrayEquals(before,Files.readAllBytes(file(world)));try(var files=Files.list(file(world).getParent())){assertEquals(1,files.count());}
    }
    @Test void absentAndLegacyFilesRemainUntouched()throws Exception {
        Path world=temp.resolve("旧格式");LegacyAccessFormat.prepare(world,s->fail());assertFalse(Files.exists(file(world)));
        CompoundTag root=new CompoundTag(),data=new CompoundTag();data.put("parts",new ListTag());root.put("data",data);write(world,root);byte[] before=Files.readAllBytes(file(world));LegacyAccessFormat.prepare(world,s->fail());assertArrayEquals(before,Files.readAllBytes(file(world)));
    }
}
