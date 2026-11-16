package cn.quit5700.stationaccess.client;
import cn.quit5700.stationaccess.*;
import com.google.gson.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.TagParser;
import java.util.*;
public final class AccessClient implements ClientModInitializer {
 public void onInitializeClient(){RangeController.register();ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->ClientAreas.areas=List.of());ClientPlayNetworking.registerGlobalReceiver(AccessNetwork.Snapshot.ID,(client,handler,buffer,response)->{String json=buffer.readUtf(1048576);client.execute(()->receive(client,json));});}
 public static void sendRequest(String json){var buffer=PacketByteBufs.create();buffer.writeUtf(json,4096);ClientPlayNetworking.send(AccessNetwork.Request.ID,buffer);}
 public static ItemStack stack(String json){try{return ItemStack.of(TagParser.parseTag(json));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException invalid){throw new IllegalArgumentException(invalid);}}
 private static void receive(Minecraft mc,String data){var json=JsonParser.parseString(data).getAsJsonObject();if(json.get("mode").getAsString().equals("selection")){RangeController.selection(json);return;}if(json.get("mode").getAsString().equals("selection_edit")){Minecraft.getInstance().setScreen(null); return;}if(json.get("mode").getAsString().equals("areas")){var batch=Arrays.asList(AccessData.GSON.fromJson(json.get("areas"),Area[].class));var all=new ArrayList<Area>();if(!json.get("reset").getAsBoolean())all.addAll(ClientAreas.areas);all.addAll(batch);ClientAreas.areas=List.copyOf(all);return;}if(mc.screen instanceof RangeListScreen list&&list.nonce().equals(json.get("nonce").getAsString()))list.update(json);else if(mc.screen instanceof AccessScreen screen&&screen.nonce().equals(json.get("nonce").getAsString()))screen.update(json);else mc.setScreen(json.get("mode").getAsString().equals("setter")?new RangeListScreen(json):new AccessScreen(json));}
}
