package cn.quit5700.stationaccess;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
public final class Ticket {
 public static String id(ItemStack item){return item.is(StationAccess.TICKET)&&item.hasTag()?item.getTag().getString("station_ticket"):"";}
 public static int displayBalance(ItemStack item){return item.hasTag()?item.getTag().getInt("display_balance"):0;}
 public static boolean issued(ItemStack item,AccessData data){return item.is(StationAccess.TICKET)&&data.state.wallets.issued(id(item));}
 public static boolean topUp(ItemStack item,AccessData data){if(!item.is(StationAccess.TICKET))return false;String id=id(item);if(id.isEmpty())id=UUID.randomUUID().toString();if(!data.state.wallets.recharge(id))return false;item.getOrCreateTag().putString("station_ticket",id);refresh(item,data);data.setDirty();return true;}
 public static void refresh(ItemStack item,AccessData data){if(!item.is(StationAccess.TICKET))return;int balance=data.state.wallets.balance(id(item));var tag=item.getOrCreateTag();var lore=new ListTag();lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("ticket.station_access.balance",balance))));lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("ticket.station_access.fare"))));var display=tag.getCompound("display");if(!lore.equals(display.getList("Lore",8))){display.put("Lore",lore);tag.put("display",display);}if(!id(item).isEmpty()&&tag.getInt("display_balance")!=balance)tag.putInt("display_balance",balance);}
 public static ItemStack find(ServerPlayer player,AccessData data,boolean positive){for(int i=0;i<player.getInventory().getContainerSize();i++){var item=player.getInventory().getItem(i);if(issued(item,data)&&(!positive||data.state.wallets.balance(id(item))>=1))return item;}return ItemStack.EMPTY;}
}
