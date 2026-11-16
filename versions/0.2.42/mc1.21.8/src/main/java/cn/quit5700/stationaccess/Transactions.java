package cn.quit5700.stationaccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
public final class Transactions {
 public static String recharge(ServerPlayer p,int slot,AccessData data){
  if(slot<0||slot>=36||!p.getInventory().getItem(slot).is(StationAccess.TICKET))return "choose_ticket";
  int emerald=-1;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(Items.EMERALD)){emerald=i;break;}
  if(emerald<0)return "emerald";
  if(!Ticket.topUp(p.getInventory().getItem(slot),data))return "limit";
  p.getInventory().getItem(emerald).shrink(1);p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();return "recharged";
 }

 public static net.minecraft.world.item.ItemStack heldTicket(ServerPlayer p){
  if(p.getMainHandItem().is(StationAccess.TICKET))return p.getMainHandItem();
  if(p.getOffhandItem().is(StationAccess.TICKET))return p.getOffhandItem();
  return net.minecraft.world.item.ItemStack.EMPTY;
 }
 public static int heldSlot(ServerPlayer p){var held=heldTicket(p);if(held.isEmpty())return -1;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i)==held)return i;return -1;}
 public static int purchaseSlot(ServerPlayer p){
  if(!heldTicket(p).isEmpty())return heldSlot(p);
  for(int slot=0;slot<36;slot++){
   if(p.getInventory().getItem(slot).is(StationAccess.TICKET))return slot;
  }
  return -1;
 }
 public record PurchaseResult(String message,int ticketSlot){}
 public static String purchase(ServerPlayer p,AccessData data){return purchaseDetailed(p,data).message();}
 public static PurchaseResult purchaseDetailed(ServerPlayer p,AccessData data){
  int slot=purchaseSlot(p);
  var target=slot>=0?p.getInventory().getItem(slot):heldTicket(p);
  boolean issue=target.isEmpty();int empty=-1;
  if(issue){for(int i=0;i<36;i++)if(p.getInventory().getItem(i).isEmpty()){empty=i;break;}if(empty<0)return new PurchaseResult("inventory_full",-1);}
  int emerald=-1;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(Items.EMERALD)){emerald=i;break;}
  if(emerald<0)return new PurchaseResult("emerald",-1);
  if(issue)target=new net.minecraft.world.item.ItemStack(StationAccess.TICKET);
  if(!Ticket.topUp(target,data))return new PurchaseResult("limit",-1);
  if(issue)p.getInventory().setItem(empty,target);
  p.getInventory().getItem(emerald).shrink(1);p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();
  return new PurchaseResult(issue?"purchased":"recharged",issue?empty:slot);
 }
}
