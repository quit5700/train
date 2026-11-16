package cn.quit5700.stationaccess;
import com.google.gson.*;
import com.mojang.serialization.*;
import cn.quit5700.stationaccess.core.Wallets;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.*;
import java.util.*;
public final class AccessData extends SavedData {
 public static final Gson GSON=new Gson();
 public static final Codec<AccessData> CODEC=Codec.STRING.listOf().xmap(parts->new AccessData(String.join("",parts)),d->chunks(GSON.toJson(d.state)));
 public static final class State { public int logicalBoundaryVersion; public List<Area> areas=new ArrayList<>(),retired=new ArrayList<>();public Wallets wallets=new Wallets();public Map<String,Set<String>> admissions=new HashMap<>(); }
 public static List<String> chunks(String text){var out=new ArrayList<String>();for(int i=0;i<text.length();i+=4000)out.add(text.substring(i,Math.min(text.length(),i+4000)));return out;}
 public State state=new State();
 public AccessData(){}
 private AccessData(String json){state=GSON.fromJson(json,State.class);if(state==null||state.areas==null||state.retired==null||state.wallets==null||state.wallets.balances==null)throw new IllegalStateException("Invalid station access saved data; refusing to discard it");for(var a:state.areas)if(a.box==null||!a.box.valid()||a.holes==null)throw new IllegalStateException("Invalid access area");}
 public static void prepare(MinecraftServer s){LegacyAccessFormat.prepare(s.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT),json->new AccessData(json));}
 public static AccessData get(MinecraftServer s){return s.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(AccessData::new,AccessData::load,null),"station_access_access");}
 private static AccessData load(net.minecraft.nbt.CompoundTag tag){var list=tag.getList("parts",8);StringBuilder text=new StringBuilder();for(int i=0;i<list.size();i++)text.append(list.getString(i));return new AccessData(text.toString());}
 @Override public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag){var list=new net.minecraft.nbt.ListTag();for(String part:chunks(GSON.toJson(state)))list.add(net.minecraft.nbt.StringTag.valueOf(part));tag.put("parts",list);return tag;}

}
