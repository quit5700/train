package cn.quit5700.stationaccess;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
public final class StationAccess  {
 public static final String ID="station_access";
 public static Item SETTER,TICKET,TERMINAL_ITEM;public static Block TERMINAL,BOUNDARY;
 public static Identifier id(String path){return Identifier.fromNamespaceAndPath(ID,path);}
 public static void initialize(){
  TERMINAL=Registry.register(BuiltInRegistries.BLOCK,id("terminal"),new TerminalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.5f).setId(ResourceKey.create(Registries.BLOCK,id("terminal")))));
  BOUNDARY=Registry.register(BuiltInRegistries.BLOCK,id("boundary"),new BoundaryBlock(BlockBehaviour.Properties.of().replaceable().strength(-1,3600000).noOcclusion().dynamicShape().noLootTable().noTerrainParticles().pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK).setId(ResourceKey.create(Registries.BLOCK,id("boundary")))));
  TERMINAL_ITEM=Registry.register(BuiltInRegistries.ITEM,id("terminal"),new BlockItem(TERMINAL,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id("terminal")))));
  TICKET=Registry.register(BuiltInRegistries.ITEM,id("ticket"),new Item(new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM,id("ticket")))));
  SETTER=Registry.register(BuiltInRegistries.ITEM,id("setter"),new SetterItem(new Item.Properties().durability(256).setId(ResourceKey.create(Registries.ITEM,id("setter")))));
  UseBlockCallback.EVENT.register((player,world,hand,hit)->setterList(player));
  UseEntityCallback.EVENT.register((player,world,hand,entity,hit)->setterList(player));
  PlayerBlockBreakEvents.AFTER.register((level,p,pos,state,be)->{if(p.isCreative())AccessService.openHole(level,pos);});
  AccessNetwork.register();HelpCompat.register();BoundaryWork.register();
  ServerTickEvents.END_SERVER_TICK.register(AccessService::tick);
  ServerLifecycleEvents.SERVER_STOPPED.register(s->{AccessService.clear();AccessNetwork.clear();BoundaryWork.clear();});
  SmokeChecks.register();
 }
 private static net.minecraft.world.InteractionResult setterList(net.minecraft.world.entity.player.Player player){
  net.minecraft.world.InteractionHand hand=player.getMainHandItem().is(SETTER)?net.minecraft.world.InteractionHand.MAIN_HAND:player.getOffhandItem().is(SETTER)?net.minecraft.world.InteractionHand.OFF_HAND:null;
  if(hand==null)return net.minecraft.world.InteractionResult.PASS;
  if(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)AccessNetwork.openSetter(serverPlayer,hand);
  return net.minecraft.world.InteractionResult.SUCCESS;
 }

}
