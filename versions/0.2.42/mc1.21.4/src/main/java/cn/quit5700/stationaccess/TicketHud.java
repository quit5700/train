package cn.quit5700.stationaccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import java.util.*;
public final class TicketHud {
 private record Seen(ItemStack item,int balance){}
 private static final Map<UUID,Seen> SEEN=new HashMap<>();
 public static void tick(ServerPlayer p,AccessData data){
  ItemStack item=Transactions.heldTicket(p);if(item.isEmpty()){SEEN.remove(p.getUUID());return;}
  int balance=data.state.wallets.balance(Ticket.id(item));Seen before=SEEN.get(p.getUUID());
  if(before==null||before.item()!=item||before.balance()!=balance){p.displayClientMessage(Component.translatable("message.station_access.held_balance",balance),true); SEEN.put(p.getUUID(),new Seen(item,balance));}
 }
 public static void retain(Set<UUID> present){SEEN.keySet().retainAll(present);}
 public static void clear(){SEEN.clear();}
}
