package cn.quit5700.stationaccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
public final class Ticket {
 public static String id(ItemStack item){if(!item.is(StationAccess.TICKET))return "";var d=item.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();return d.getString("station_ticket");}
 public static boolean issued(ItemStack item,AccessData data){return item.is(StationAccess.TICKET)&&data.state.wallets.issued(id(item));}
 public static boolean topUp(ItemStack item,AccessData data){
  if(!item.is(StationAccess.TICKET))return false;String id=id(item);if(id.isEmpty())id=UUID.randomUUID().toString();
  if(!data.state.wallets.recharge(id))return false;var tag=item.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putString("station_ticket",id);item.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));refresh(item,data);data.setDirty();return true;
 }
 public static void refresh(ItemStack item,AccessData data){if(!item.is(StationAccess.TICKET))return;var lore=new ItemLore(List.of(Component.translatable("ticket.station_access.balance",data.state.wallets.balance(id(item))),Component.translatable("ticket.station_access.fare")));if(!lore.equals(item.get(DataComponents.LORE)))item.set(DataComponents.LORE,lore);var tag=item.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();if(!id(item).isEmpty()&&tag.getInt("display_balance")!=data.state.wallets.balance(id(item))){tag.putInt("display_balance",data.state.wallets.balance(id(item)));item.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}}
 public static ItemStack find(ServerPlayer player,AccessData data,boolean positive){for(int i=0;i<player.getInventory().getContainerSize();i++){ItemStack s=player.getInventory().getItem(i);if(issued(s,data)&&(!positive||data.state.wallets.balance(id(s))>=1))return s;}return ItemStack.EMPTY;}
}
