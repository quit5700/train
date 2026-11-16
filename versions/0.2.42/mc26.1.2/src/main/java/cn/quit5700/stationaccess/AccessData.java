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
 public static final SavedDataType<AccessData> TYPE=new SavedDataType<>(StationAccess.id("access"),AccessData::new,CODEC,null);
 public static final class State { public int logicalBoundaryVersion; public List<Area> areas=new ArrayList<>(),retired=new ArrayList<>();public Wallets wallets=new Wallets();public Map<String,Set<String>> admissions=new HashMap<>(); }
 public static List<String> chunks(String text){var out=new ArrayList<String>();for(int i=0;i<text.length();i+=4000)out.add(text.substring(i,Math.min(text.length(),i+4000)));return out;}
 public State state=new State();
 public AccessData(){}
 private AccessData(String json){state=GSON.fromJson(json,State.class);if(state==null||state.areas==null||state.retired==null||state.wallets==null||state.wallets.balances==null)throw new IllegalStateException("Invalid station access saved data; refusing to discard it");for(var a:state.areas)if(a.box==null||!a.box.valid()||a.holes==null)throw new IllegalStateException("Invalid access area");}
 public static AccessData get(MinecraftServer s){return s.overworld().getDataStorage().computeIfAbsent(TYPE);}
}
