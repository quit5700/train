package cn.quit5700.fabrictrain.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Keeps railway data readable when Minecraft versions use different SavedData paths. */
public final class RailwayDataMigration {
    private static final Logger LOGGER=LoggerFactory.getLogger(RailwayDataMigration.class);
    private static final String FILE_NAME="fabric_train_simulator_railway_data.dat",BACKUP_SUFFIX=".pre-0.2.36.bak",RECOVERY_SUFFIX=".recovery.bak";
    private RailwayDataMigration(){}
    public static void prepare(MinecraftServer server,boolean namespacedCanonical){migrate(server.getWorldPath(LevelResource.ROOT),namespacedCanonical);}
    static void migrate(Path root,boolean namespaced){
        Path flat=root.resolve("data").resolve(FILE_NAME),nested=root.resolve("data").resolve("fabric_train_simulator").resolve("railway_data.dat"),dimension=root.resolve("dimensions").resolve("minecraft").resolve("overworld").resolve("data").resolve("fabric_train_simulator").resolve("railway_data.dat"),canonical=namespaced?dimension:flat;
        try{
            List<Path> candidates=new ArrayList<>();
            for(Path path:List.of(flat,nested,dimension)){candidates.add(path);candidates.add(path.resolveSibling(path.getFileName()+BACKUP_SUFFIX));candidates.add(path.resolveSibling(path.getFileName()+RECOVERY_SUFFIX));candidates.add(path.resolveSibling(path.getFileName()+"_old"));}
            Path best=authoritative(canonical)?canonical:bestExisting(candidates);if(best==null)return;backup(best);
            if(!best.equals(canonical)&&!authoritative(canonical)&&isBetter(best,canonical)){backup(canonical);Files.createDirectories(canonical.getParent());Files.copy(best,canonical,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.COPY_ATTRIBUTES);LOGGER.info("已自动恢复铁路线路存档：{} -> {}",best,canonical);}
            Path effective=Files.isRegularFile(canonical)?canonical:best;
            if(contentScore(effective)>0){Path recovery=canonical.resolveSibling(canonical.getFileName()+RECOVERY_SUFFIX);Files.createDirectories(recovery.getParent());Files.copy(effective,recovery,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.COPY_ATTRIBUTES);}
        }catch(IOException e){LOGGER.error("自动迁移铁路线路存档失败；原文件未删除：{}",root,e);}
    }
    private static boolean authoritative(Path path){
        if(!Files.isRegularFile(path))return false;
        try{if(contentScore(path)>0)return true;
            try(var in=new DataInputStream(new GZIPInputStream(Files.newInputStream(path)))){if(in.readUnsignedByte()!=10)return false;readString(in);return schema(in,0);}
        }catch(IOException|RuntimeException invalid){return false;}
    }
    private static boolean schema(DataInputStream in,int depth)throws IOException{
        boolean found=false;while(true){int type=in.readUnsignedByte();if(type==0)return found;String name=readString(in);
            if(type==3&&name.equals("SchemaVersion")&&depth<=1){found|=in.readInt()>0;}
            else if(type==10&&depth==0&&name.equals("data")){found|=schema(in,1);}else skip(in,type,depth+1);
        }
    }
    private static Path bestExisting(List<Path> paths){return paths.stream().distinct().filter(Files::isRegularFile).filter(p->{try{return Files.size(p)>0;}catch(IOException e){return false;}}).max(Comparator.comparingInt(RailwayDataMigration::safeScore).thenComparing(p->{try{return Files.getLastModifiedTime(p);}catch(IOException e){return java.nio.file.attribute.FileTime.fromMillis(0);}})).orElse(null);}
    private static boolean isBetter(Path candidate,Path canonical)throws IOException{if(!Files.isRegularFile(canonical)||Files.size(canonical)==0)return true;int a=contentScore(candidate),b=contentScore(canonical);return a>b||a==b&&Files.getLastModifiedTime(candidate).compareTo(Files.getLastModifiedTime(canonical))>0;}
    private static int safeScore(Path path){try{return contentScore(path);}catch(IOException e){return 0;}}
    /** Scores actual device and line entries; a newer empty save cannot replace useful data. */
    static int contentScore(Path path)throws IOException{
        try(InputStream file=Files.newInputStream(path);InputStream gzip=new GZIPInputStream(file);DataInputStream in=new DataInputStream(gzip)){if(in.readUnsignedByte()!=10)return 0;readString(in);return scoreCompound(in,0);}catch(RuntimeException e){return 0;}
    }
    private static int scoreCompound(DataInputStream in,int depth)throws IOException{if(depth>64)throw new IOException("NBT nesting is too deep");int score=0;while(true){int type=in.readUnsignedByte();if(type==0)return score;String name=readString(in);if(type==9){int child=in.readUnsignedByte(),length=length(in.readInt());if("Devices".equals(name))score+=length*1000;if("Lines".equals(name))score+=length*1000000;for(int i=0;i<length;i++)score+=skip(in,child,depth+1);}else score+=skip(in,type,depth+1);}}
    private static int skip(DataInputStream in,int type,int depth)throws IOException{return switch(type){case 1->{in.readByte();yield 0;}case 2->{in.readShort();yield 0;}case 3->{in.readInt();yield 0;}case 4->{in.readLong();yield 0;}case 5->{in.readFloat();yield 0;}case 6->{in.readDouble();yield 0;}case 7->{in.skipNBytes(length(in.readInt()));yield 0;}case 8->{readString(in);yield 0;}case 9->{int child=in.readUnsignedByte(),count=length(in.readInt()),score=0;for(int i=0;i<count;i++)score+=skip(in,child,depth+1);yield score;}case 10->scoreCompound(in,depth+1);case 11->{in.skipNBytes((long)length(in.readInt())*4);yield 0;}case 12->{in.skipNBytes((long)length(in.readInt())*8);yield 0;}default->throw new IOException("Unknown NBT tag type: "+type);};}
    private static int length(int value)throws IOException{if(value<0||value>10000000)throw new IOException("Invalid NBT length: "+value);return value;}
    private static String readString(DataInputStream in)throws IOException{int length=in.readUnsignedShort();return new String(in.readNBytes(length),StandardCharsets.UTF_8);}
    private static void backup(Path source)throws IOException{if(!Files.isRegularFile(source))return;Path target=source.resolveSibling(source.getFileName()+BACKUP_SUFFIX);if(!Files.exists(target))Files.copy(source,target,StandardCopyOption.COPY_ATTRIBUTES);}
}
