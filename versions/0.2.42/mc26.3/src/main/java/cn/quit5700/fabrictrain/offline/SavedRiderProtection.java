package cn.quit5700.fabrictrain.offline;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;

/** Reads the singleplayer host's vanilla root vehicle before legacy train virtualisation. */
public final class SavedRiderProtection {
    private SavedRiderProtection() {}
    public static Set<UUID> savedTrains(MinecraftServer server) {
        var profile=server.getSingleplayerProfile();
        if(profile==null)return Set.of();
        try {
            Path root=server.getWorldPath(LevelResource.ROOT);
            Path level=root.resolve("level.dat");
            if(Files.isRegularFile(level)){
                CompoundTag data=compound(read(level),"Data");
                CompoundTag player=data==null?null:compound(data,"Player");
                // Inline player data is authoritative, including an explicit absence of RootVehicle.
                if(player!=null)return fromPlayer(player);
            }
            UUID id=null;
            for(String getter:List.of("id","getId")){
                try{Object value=profile.getClass().getMethod(getter).invoke(profile);if(value instanceof UUID uuid){id=uuid;break;}}
                catch(NoSuchMethodException ignored){}
            }
            if(id==null)return Set.of();
            for(String folder:List.of("players/data","playerdata")){
                Path player=root.resolve(folder).resolve(id+".dat");
                if(Files.isRegularFile(player))return fromPlayer(read(player));
            }
        }catch(Exception error){org.slf4j.LoggerFactory.getLogger(SavedRiderProtection.class).warn("Unable to inspect saved host vehicle before train restore",error);}
        return Set.of();
    }
    private static CompoundTag read(Path path) throws java.io.IOException { return NbtIo.readCompressed(path,NbtAccounter.unlimitedHeap()); }
    private static CompoundTag compound(CompoundTag parent,String key){return parent.get(key) instanceof CompoundTag tag?tag:null;}
    public static Set<UUID> fromPlayer(CompoundTag player){
        var root=compound(player,"RootVehicle");var entity=root==null?null:compound(root,"Entity");
        if(entity==null)return Set.of();
        String type=string(entity,"id");
        if(!type.startsWith("fabric_train_simulator:"))return Set.of();
        try{return Set.of(UUID.fromString(string(entity,"TrainId")));}catch(IllegalArgumentException error){return Set.of();}
    }
    private static String string(CompoundTag parent,String key){var tag=parent.get(key);return tag==null?"":Codec.STRING.parse(NbtOps.INSTANCE,tag).result().orElse("");}
    public static void protect(InfrastructureData root,Set<UUID> saved){
        for(UUID id:saved){var train=root.trains.get(id);
            if(train!=null&&!train.virtual()&&!train.occupied()&&!train.members().isEmpty())
                root.put(new InfrastructureData.Owned(train.id(),train.origin(),train.state(),false,train.members(),true));
        }
    }
}
