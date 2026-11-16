package cn.quit5700.stationaccess;

import cn.quit5700.stationaccess.core.Box;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Explicitly opt-in checks in a dedicated disposable world, never in ordinary play. */
public final class SmokeChecks {
    private static final String MODE=System.getProperty("station_access.smoke","");
    private static final List<String> CHECKS=new ArrayList<>();
    private static int phase;
    private static ProbePlayer manualSwitchRider;
    private static final java.util.List<ItemEntity> observedDrops=new java.util.ArrayList<>();
    private static Area area;
    private static final BlockPos WALL=new BlockPos(0,81,2),BARRIER=new BlockPos(0,81,3);
    private static final class ProbePlayer extends ServerPlayer {
        boolean creative,spectator;int overlayMessages;
        ProbePlayer(MinecraftServer server){super(server,server.overworld(),new GameProfile(UUID.randomUUID(),"AccessProbe"),ClientInformation.createDefault());}
        @Override public void rideTick() {
            if (connection != null) super.rideTick();
            else if (getVehicle() != null) setPos(getVehicle().getX(), getVehicle().getY(), getVehicle().getZ());
        }
        // ProbePlayer has no socket. ServerPlayer attaches/detaches correctly before its final network-only notification.
        @Override public void stopRiding(){try{super.stopRiding();}catch(NullPointerException transport){if(connection!=null||isPassenger())throw transport;}}
        @Override public boolean isCreative(){return creative;}
        @Override public boolean isSpectator(){return spectator;}
        @Override public void teleportTo(double x,double y,double z){setPos(x,y,z);}
        @Override public void displayClientMessage(Component message,boolean overlay){overlayMessages++;}
    }
    public static void register(){if(!MODE.isEmpty()){ServerTickEvents.END_SERVER_TICK.register(SmokeChecks::tick);net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{if(entity instanceof ItemEntity item)observedDrops.add(item);});}}
    private static void check(boolean pass,String name){if(!pass)throw new AssertionError(name);CHECKS.add(name);}
    private static void tick(MinecraftServer server){
        if(phase==99||server.getTickCount()<40)return;
        try {
            var world=server.overworld();var data=AccessData.get(server);
            if(MODE.equals("reload")){
                check(data.state.areas.size()==1,"reload_area_persisted");check(!data.state.areas.get(0).visible()&&data.state.areas.get(0).color==14,"reload_shared_visibility_color");check(data.state.areas.get(0).anchor().equals(new Box.Cell(6,85,6)),"reload_original_first_point_not_normalized");
                check(data.state.areas.get(0).holes.contains(BARRIER.asLong()),"reload_creative_gap_persisted");
                check(data.state.wallets.balances.containsKey("reload-balance")&&data.state.wallets.balance("reload-balance")==19,"reload_server_balance_persisted");
                check(!data.state.admissions.isEmpty(),"reload_paid_admission_persisted");
                var depot=(cn.quit5700.fabrictrain.block.entity.TrainDepotBlockEntity)world.getBlockEntity(new BlockPos(24,81,24));
                check(depot!=null&&depot.maximumTrains()==0,"reload_zero_train_maximum_persisted");
                check(depot.pendingRecalls().contains(new UUID(888,999)),"reload_pending_unloaded_recall_persisted");
                var railway=cn.quit5700.fabrictrain.data.RailwayDataManager.state(world);var station=railway.find(cn.quit5700.fabrictrain.data.RailwayDataManager.dimension(world),29,81,24);
                check(station!=null&&Boolean.FALSE.equals(railway.stationContainer(station.id())),"reload_station_container_state_persisted");
                done(server,true,"");return;
            }
            if (MODE.equals("manual-switch") || phase == 3) {
                if (manualSwitchRider == null) manualSwitchRider = new ProbePlayer(server);
                if (ManualSwitchSmoke.tick(world, server.getTickCount(), manualSwitchRider,
                        vehicle -> board(manualSwitchRider, vehicle), SmokeChecks::check)) done(server,true,"");
                return;
            }
            if(phase==0){
                world.getChunkAt(WALL);
                area=new Area(UUID.randomUUID().toString(),AccessService.dimension(world),new Box(0,80,0,6,85,6));
                area.first=new Box.Cell(6,85,6);area.second=new Box.Cell(0,80,0);area.shown=false;area.color=14;data.state.areas.add(area);data.setDirty();BoundaryWork.refresh();
                world.setBlock(WALL,Blocks.STONE.defaultBlockState(),3);world.setBlock(BARRIER,StationAccess.BOUNDARY.defaultBlockState(),3);world.setBlock(new BlockPos(9,81,3),StationAccess.BOUNDARY.defaultBlockState(),3);world.setBlock(new BlockPos(15,84,15),StationAccess.BOUNDARY.defaultBlockState(),3);BoundaryWork.schedule(world,world.getChunkAt(WALL));
                phase=1;return;
            }
            if(phase==1&&server.getTickCount()>=48){
                check(world.getBlockState(WALL).is(Blocks.STONE),"existing_wall_not_replaced");
                check(world.getBlockState(new BlockPos(3,82,3)).isAir(),"interior_stays_hollow");
                check(world.getBlockState(BARRIER).isAir(),"logical_surface_never_occupies_world");
                check(BoundaryWork.tick(server,data)<=128,"per_tick_work_budget");check(world.getBlockState(new BlockPos(9,81,3)).isAir(),"orphan_legacy_boundary_removed");check(world.setBlock(BARRIER,Blocks.STONE.defaultBlockState(),3)&&world.getBlockState(BARRIER).is(Blocks.STONE),"can_build_on_logical_surface");world.removeBlock(BARRIER,false);check(world.getBlockState(BARRIER).isAir(),"logical_surface_does_not_regenerate");world.setBlock(new BlockPos(15,84,15),Blocks.STONE.defaultBlockState(),3);for(int i=0;i<48;i++)if(BoundaryWork.tick(server,data)>128)throw new AssertionError("cleanup budget");check(world.getBlockState(new BlockPos(15,84,15)).is(Blocks.STONE),"cleanup_never_deletes_replacement_building");
                net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(world.enabledFeatures(),true,world.registryAccess());
                var trainTab=cn.quit5700.fabrictrain.registry.ModItemGroup.TRAINS;
                var originalItems=List.of(cn.quit5700.fabrictrain.registry.ModItems.SIGNAL_RAIL,cn.quit5700.fabrictrain.registry.ModItems.SWITCH_RAIL,cn.quit5700.fabrictrain.registry.ModItems.POWER_GENERATOR,cn.quit5700.fabrictrain.registry.ModItems.TRAIN_DEPOT,cn.quit5700.fabrictrain.registry.ModItems.STATION_MARKER,cn.quit5700.fabrictrain.registry.ModItems.LOCOMOTIVE,cn.quit5700.fabrictrain.registry.ModItems.PASSENGER_CAR,cn.quit5700.fabrictrain.registry.ModItems.CARGO_CAR,cn.quit5700.fabrictrain.registry.ModItems.LINE_SETTER,cn.quit5700.fabrictrain.registry.ModItems.TRAIN_CONNECTOR);
                for(Item item:originalItems)check(trainTab.getDisplayItems().stream().anyMatch(stack->stack.is(item)),"original_train_item_in_own_tab_"+item);
                for(Item item:List.of(StationAccess.SETTER,StationAccess.TICKET,StationAccess.TERMINAL_ITEM))check(trainTab.getDisplayItems().stream().anyMatch(stack->stack.is(item)),"access_item_in_train_tab_"+item);
                for(var vehicle:List.of(new cn.quit5700.fabrictrain.entity.LocomotiveEntity(cn.quit5700.fabrictrain.registry.ModEntities.LOCOMOTIVE,world),new cn.quit5700.fabrictrain.entity.PassengerCarEntity(cn.quit5700.fabrictrain.registry.ModEntities.PASSENGER_CAR,world),new cn.quit5700.fabrictrain.entity.CargoCarEntity(cn.quit5700.fabrictrain.registry.ModEntities.CARGO_CAR,world))){
                    vehicle.setPos(-1.5,82,4.5);AccessService.auditVehicle(vehicle,data);vehicle.setPos(2,82,4.5);AccessService.auditVehicle(vehicle,data);check(vehicle.getX()==2,"actual_train_vehicle_crosses_permission_layer_"+vehicle.getType());
                    check(!world.getBlockState(WALL).getCollisionShape(world,WALL,CollisionContext.of(vehicle)).isEmpty(),"actual_train_vehicle_cannot_cross_wall_"+vehicle.getType());
                }

                check(StationAccess.BOUNDARY.defaultBlockState().getPistonPushReaction()==net.minecraft.world.level.material.PushReaction.BLOCK,"boundary_pistons_cannot_move");
                var p=new ProbePlayer(server);p.setPos(-1.5,81,3.5);
                AccessService.audit(p,data);p.setPos(2,81,3.5);AccessService.audit(p,data);
                check(p.getX()<0,"ticketless_entry_rejected");
                p.getInventory().setItem(0,new ItemStack(StationAccess.SETTER));p.setPos(2,81,3.5);AccessService.audit(p,data);
                check(p.getX()<0,"own_tool_does_not_waive_ticket");
                ItemStack card=new ItemStack(StationAccess.TICKET);p.getInventory().setItem(0,card);
                check(Transactions.recharge(p,0,data).equals("emerald"),"no_emerald_no_topup");
                p.getInventory().setItem(1,new ItemStack(Items.EMERALD,2));
                check(Transactions.recharge(p,0,data).equals("recharged"),"terminal_recharges_inventory_card");
                check(data.state.wallets.balance(Ticket.id(card))==10&&p.getInventory().getItem(1).getCount()==1,"one_emerald_is_ten_credits");
                check(Transactions.recharge(p,35,data).equals("choose_ticket")&&p.getInventory().getItem(1).getCount()==1,"invalid_slot_never_consumes_currency");
                check(AccessService.canCross(p,area),"valid_card_opens_permission_collision");
                check(!world.getBlockState(WALL).getCollisionShape(world,WALL,CollisionContext.of(p)).isEmpty(),"ticket_never_allows_wall_phasing");
                p.setPos(.2,81,3.5);AccessService.audit(p,data);check(data.state.wallets.balance(Ticket.id(card))==10,"touching_shell_is_not_entry");p.setPos(-.2,81,3.5);AccessService.audit(p,data);p.setPos(.2,81,3.5);AccessService.audit(p,data);check(data.state.wallets.balance(Ticket.id(card))==10,"boundary_jitter_no_duplicate_fare");
                p.setPos(2,81,3.5);AccessService.audit(p,data);check(data.state.wallets.balance(Ticket.id(card))==10,"enter_validates_without_charge");
                p.setPos(3,81,3.5);AccessService.audit(p,data);check(data.state.wallets.balance(Ticket.id(card))==10,"moving_inside_does_not_charge");
                data.state.wallets.balances.put(Ticket.id(card),0);AccessService.clear();AccessService.audit(p,data);check(p.getX()==3&&data.state.wallets.balance(Ticket.id(card))==0,"paid_rejoin_inside_does_not_charge_or_eject");p.setPos(-1.5,81,3.5);AccessService.audit(p,data);
                check(p.getX()<0&&data.state.wallets.balance(Ticket.id(card))==0,"zero_balance_exit_is_free");
                p.setPos(2,81,3.5);AccessService.audit(p,data);check(p.getX()<0,"zero_balance_reentry_rejected");
                p.creative=true;p.getInventory().setItem(0,ItemStack.EMPTY);p.setPos(2,81,3.5);AccessService.audit(p,data);check(p.getX()<0,"creative_not_automatically_exempt");
                p.spectator=true;p.setPos(2,81,3.5);AccessService.audit(p,data);check(p.getX()==2,"spectator_passes_without_ticket");p.spectator=false;p.creative=false;
                check(!AccessNetwork.breach(p,BARRIER),"survival_cannot_remove_logical_permission");check(PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(world,p,WALL,world.getBlockState(WALL),null),"survival_can_break_ordinary_wall_on_surface");
                String owner=area.owner;area.owner=p.getUUID().toString();check(AccessService.manager(p,area),"owner_can_manage");check(!AccessService.manager(new ProbePlayer(server),area),"stranger_cannot_manage");area.owner=owner;
                ItemStack tool=new ItemStack(StationAccess.SETTER);p.getInventory().setItem(0,tool);check(tool.getMaxDamage()==256,"tool_has_256_durability");AccessNetwork.wearTool(p,net.minecraft.world.InteractionHand.MAIN_HAND,tool);check(tool.getDamageValue()==1,"survival_success_costs_one_durability");p.creative=true;AccessNetwork.wearTool(p,net.minecraft.world.InteractionHand.MAIN_HAND,tool);check(tool.getDamageValue()==1,"creative_does_not_consume_durability");p.creative=false;
                p.creative=true;check(PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(world,p,BARRIER,world.getBlockState(BARRIER),null),"creative_can_break_surface");p.creative=false;
                for(Item item:List.of(StationAccess.SETTER,StationAccess.TICKET,StationAccess.TERMINAL_ITEM)){
                    var entity=new ItemEntity(world,-1.5,81,3.5,new ItemStack(item));
                    entity.setPos(-1.5,82,4.5);AccessService.auditVehicle(entity,data);entity.setPos(2,82,4.5);AccessService.auditVehicle(entity,data);check(entity.getX()==2,"own_item_passes_"+item);
                }
                var stone=new ItemEntity(world,-1.5,81,3.5,new ItemStack(Items.STONE));stone.setPos(-1.5,82,4.5);AccessService.auditVehicle(stone,data);stone.setPos(2,82,4.5);AccessService.auditVehicle(stone,data);check(stone.getX()<0,"vanilla_item_stays_blocked");
                p.creative=true;p.setPos(-1.5,81,3.5);check(AccessNetwork.breach(p,BARRIER),"creative_can_open_empty_logical_surface");p.creative=false;check(AccessService.at(world,BARRIER)==null,"creative_gap_is_open");check(AccessService.at(world,WALL)!=null,"other_boundary_cells_remain_active");
                var large=new AccessData();var big=new Area("owner",AccessService.dimension(world),new Box(0,0,0,1000,100,1000));for(long i=0;i<24000;i++)big.holes.add(i*100000);large.state.areas.add(big);String encoded=AccessData.CODEC.encodeStart(JsonOps.INSTANCE,large).getOrThrow().toString();check(AccessData.CODEC.parse(JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(encoded)).getOrThrow().state.areas.get(0).holes.size()==24000,"large_saved_state_chunked_roundtrip");
                for(String id:List.of("setter","terminal"))check(server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,StationAccess.id(id))).isPresent(),"survival_recipe_loaded_"+id);{String id="ticket";check(!(server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,StationAccess.id(id))).isPresent()),"ticket_crafting_removed");}
                batch9(server);batch8(server);
                data.state.wallets.balances.put("reload-balance",19);data.state.admissions.put("reload-player",new HashSet<>(Set.of(area.id)));data.setDirty();phase=2;
            }
            if(phase==2&&server.getTickCount()>=60){if(server.getTickCount()<400&&(observedDrops.size()<2||world.getEntity(batchPassenger.getUUID())==null))return;if(!world.canSeeSky(BARRIER)&&server.getTickCount()<140)return;batch8Deferred(server);check(world.canSeeSky(BARRIER),"logical_boundary_does_not_cover_sky_after_lighting_settles");check(world.getBlockState(BARRIER).isAir(),"logical_surface_stays_air_after_more_ticks");phase=3;return;}

        }catch(Throwable failure){failure.printStackTrace();done(server,false,failure.toString());}
    }

    private static boolean board(ProbePlayer p,net.minecraft.world.entity.Entity vehicle){try{return p.startRiding(vehicle);}catch(NullPointerException transport){if(p.connection==null&&p.isPassenger())return true;throw transport;}}
    private static net.minecraft.world.entity.Entity batchPassenger,batchOther;
    private static void batch8(MinecraftServer server){
        var world=server.overworld();var access=AccessData.get(server);
        var p=new ProbePlayer(server);p.setPos(-2,82,4.5);AccessService.audit(p,access);
        var head=new cn.quit5700.fabrictrain.entity.LocomotiveEntity(cn.quit5700.fabrictrain.registry.ModEntities.LOCOMOTIVE,world);
        head.setPos(-2,82,4.5);check(board(p,head),"unpaid_player_boards_train");
        head.setPos(2,82,4.5);p.setPos(2,82,4.5);AccessService.audit(p,access);
        check(p.isPassenger()&&p.getX()==2,"unpaid_train_passenger_enters_without_ejection");
        check(AccessService.canCross(head,area),"unpaid_passenger_does_not_block_train");p.stopRiding();
        p.setPos(-2,82,4.5);AccessService.audit(p,access);check(p.getX()==-2,"ticketless_exit_is_unrestricted");
        p.setPos(2,82,4.5);AccessService.audit(p,access);check(p.getX()==-2,"ticketless_pedestrian_entry_still_denied");
        var card=new ItemStack(StationAccess.TICKET);p.getInventory().setItem(0,card);check(Ticket.topUp(card,access),"fare_test_ticket_issued");
        p.setPos(2,82,4.5);AccessService.audit(p,access);check(access.state.wallets.balance(Ticket.id(card))==10,"no_charge_on_new_pedestrian_entry");
        p.setPos(-2,82,4.5);AccessService.audit(p,access);check(access.state.wallets.balance(Ticket.id(card))==9,"exit_charges_one_credit");
        p.setPos(.2,82,4.5);AccessService.audit(p,access);p.setPos(-.2,82,4.5);AccessService.audit(p,access);check(access.state.wallets.balance(Ticket.id(card))==9,"exit_shell_jitter_does_not_charge_twice");
        head.setPos(-2,82,4.5);board(p,head);p.setPos(2,82,4.5);AccessService.audit(p,access);p.setPos(-2,82,4.5);AccessService.audit(p,access);
        check(access.state.wallets.balance(Ticket.id(card))==8&&p.isPassenger(),"riding_exit_charges_without_ejection");p.stopRiding();
        access.state.wallets.balances.put(Ticket.id(card),0);p.setPos(2,82,4.5);board(p,head);AccessService.audit(p,access);p.setPos(-2,82,4.5);AccessService.audit(p,access);p.stopRiding();
        check(p.getX()==-2&&access.state.wallets.balance(Ticket.id(card))==0,"empty_ticket_exit_never_negative");
        check(!cn.quit5700.fabrictrain.depot.AutomaticDispatchSchedule.shouldAttempt(true,true,0,0,10000,0,1),"zero_maximum_disables_periodic_dispatch");
        check(!cn.quit5700.fabrictrain.depot.AutomaticDispatchSchedule.shouldAttempt(true,true,0,-1,10000,0,1),"negative_maximum_never_dispatches");
        var pos=new BlockPos(24,81,24);world.setChunkForced(1,1,true);world.getChunkAt(pos);for(int fx=21;fx<=31;fx++)for(int fz=21;fz<=28;fz++)world.setBlock(new BlockPos(fx,80,fz),Blocks.STONE.defaultBlockState(),3);world.setBlock(pos,cn.quit5700.fabrictrain.registry.ModBlocks.TRAIN_DEPOT.defaultBlockState(),3);
        var depot=(cn.quit5700.fabrictrain.block.entity.TrainDepotBlockEntity)world.getBlockEntity(pos);
        depot.configure(0,cn.quit5700.fabrictrain.train.TrainClass.CARGO,1,0,60,100,160);check(depot.maximumTrains()==0,"zero_maximum_stays_zero");
        depot.configure(0,cn.quit5700.fabrictrain.train.TrainClass.CARGO,1,-6,60,100,160);check(depot.maximumTrains()==0,"negative_maximum_sanitized_to_zero");
        var railway=cn.quit5700.fabrictrain.data.RailwayDataManager.state(world);
        var depotDevice=cn.quit5700.fabrictrain.data.RailwayDataManager.register(world,pos,cn.quit5700.fabrictrain.data.DeviceType.DEPOT);
        var stationPos=new BlockPos(29,81,24);world.setBlock(stationPos,cn.quit5700.fabrictrain.registry.ModBlocks.STATION_MARKER.defaultBlockState(),3);
        var station=cn.quit5700.fabrictrain.data.RailwayDataManager.register(world,stationPos,cn.quit5700.fabrictrain.data.DeviceType.STATION);
        var link=head.trainLink();
        check(!cn.quit5700.fabrictrain.cargo.StationSelectionService.availableStations(world,railway,link).contains(station),"station_without_container_hidden");
        var chestPos=stationPos.east();world.setBlock(chestPos,Blocks.CHEST.defaultBlockState(),3);
        check(cn.quit5700.fabrictrain.cargo.StationSelectionService.availableStations(world,railway,link).contains(station),"connected_station_appears_immediately");
        check(cn.quit5700.fabrictrain.cargo.StationSelectionService.canSelect(world,railway,link,station.id()),"live_station_selection_accepted");
        world.removeBlock(chestPos,false);check(!cn.quit5700.fabrictrain.cargo.StationSelectionService.canSelect(world,railway,link,station.id()),"stale_selection_rejected_after_chest_removed");
        check(cn.quit5700.fabrictrain.cargo.StationSelectionService.canSelect(world,railway,link,0),"explicit_empty_station_always_allowed");
        var cargo=new cn.quit5700.fabrictrain.entity.CargoCarEntity(cn.quit5700.fabrictrain.registry.ModEntities.CARGO_CAR,world);cargo.setPos(25,82,24);cargo.setItem(0,new ItemStack(Items.DIAMOND,64));
        var rider=new ProbePlayer(server);rider.setPos(25,82,24);board(rider,cargo);
        for(int slot=0;slot<depot.getContainerSize();slot++)depot.setItem(slot,new ItemStack(Items.COBBLESTONE,64));
        world.setBlock(pos.east(),Blocks.CHEST.defaultBlockState(),3);var chest=(net.minecraft.world.Container)world.getBlockEntity(pos.east());
        for(int slot=0;slot<chest.getContainerSize();slot++)chest.setItem(slot,new ItemStack(Items.COBBLESTONE,64));chest.setItem(0,new ItemStack(Items.DIAMOND,60));
        check(cn.quit5700.fabrictrain.depot.DepotRecoveryService.recover(java.util.List.of(cargo),depot),"full_inventory_still_recovers_cargo_car");
        check(cargo.isRemoved()&&!rider.isPassenger(),"recovery_removes_car_and_dismounts_rider");
        check(chest.getItem(0).is(Items.DIAMOND)&&chest.getItem(0).getCount()==64,"recovery_fills_connected_chest_first");
        depot.clearContent();chest.clearContent();
        var passenger=new cn.quit5700.fabrictrain.entity.PassengerCarEntity(cn.quit5700.fabrictrain.registry.ModEntities.PASSENGER_CAR,world);passenger.setPos(25,82,24);world.addFreshEntity(passenger);
        passenger.trainLink().setRegistryAddress(depotDevice.id(),1,1);
        cn.quit5700.fabrictrain.train.TrainRegistryService.observeLoadedVehicle(world,passenger,passenger);
        var other=new cn.quit5700.fabrictrain.entity.PassengerCarEntity(cn.quit5700.fabrictrain.registry.ModEntities.PASSENGER_CAR,world);other.setPos(27,82,24);world.addFreshEntity(other);other.trainLink().setRegistryAddress(depotDevice.id()+999,2,1);cn.quit5700.fabrictrain.train.TrainRegistryService.observeLoadedVehicle(world,other,other);
        batchPassenger=passenger;batchOther=other;

    }

    private static void batch8Deferred(MinecraftServer server){
        var world=server.overworld();var pos=new BlockPos(24,81,24);
        var depot=(cn.quit5700.fabrictrain.block.entity.TrainDepotBlockEntity)world.getBlockEntity(pos);
        var depotDevice=cn.quit5700.fabrictrain.data.RailwayDataManager.state(world).find(cn.quit5700.fabrictrain.data.RailwayDataManager.dimension(world),24,81,24);
        int diamonds=0,cars=0;for(var drop:observedDrops){if(!drop.isRemoved()&&drop.getItem().is(Items.DIAMOND))diamonds+=drop.getItem().getCount();if(!drop.isRemoved()&&drop.getItem().is(cn.quit5700.fabrictrain.registry.ModItems.CARGO_CAR))cars+=drop.getItem().getCount();}
        check(diamonds==60&&cars==1,"overflow_drops_exact_remainder_and_vehicle_at_depot:"+diamonds+":"+cars);
        cn.quit5700.fabrictrain.depot.DepotRecallService.request(world,pos,depot,depotDevice.id());
        check(!batchPassenger.isRemoved()&&!batchOther.isRemoved(),"zero_auto_recall_preserves_manual_trains");batchPassenger.discard();
        check(depot.pendingRecalls().isEmpty(),"completed_recall_queue_cleared");batchOther.discard();
        var railway=cn.quit5700.fabrictrain.data.RailwayDataManager.state(world);var pending=new UUID(888,999);
        railway.ensureTrain(pending,true,0,cn.quit5700.fabrictrain.train.ControlMode.ROUTE_AUTOMATIC);
        railway.replaceTrainManifest(pending,depotDevice.id(),java.util.List.of(new cn.quit5700.fabrictrain.data.TrainMember(new UUID(777,999),cn.quit5700.fabrictrain.data.TrainVehicleKind.CARGO,1)));
        depot.pendingRecalls().add(pending);depot.setChanged();
        check(depot.maximumTrains()==0&&depot.pendingRecalls().size()==1,"save_zero_limit_and_pending_unloaded_recall");
    }

    private static void batch9(MinecraftServer server){
        var data=AccessData.get(server);var p=new ProbePlayer(server);p.getInventory().clearContent();
        p.getInventory().setItem(1,new ItemStack(Items.EMERALD,2));
        check(Transactions.purchase(p,data).equals("purchased"),"terminal_issues_new_ticket_without_held_ticket");
        var card=p.getInventory().getItem(0);check(Ticket.issued(card,data)&&data.state.wallets.balance(Ticket.id(card))==10,"new_ticket_has_ten_uses");
        check(Transactions.purchase(p,data).equals("recharged")&&data.state.wallets.balance(Ticket.id(card))==20&&p.getInventory().getItem(1).isEmpty(),"held_ticket_topup_consumes_exactly_one_emerald");
        check(Transactions.purchase(p,data).equals("emerald")&&data.state.wallets.balance(Ticket.id(card))==20,"no_currency_leaves_ticket_unchanged");
        for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));p.getInventory().setItem(2,new ItemStack(Items.EMERALD));
        check(Transactions.purchase(p,data).equals("inventory_full")&&p.getInventory().getItem(2).getCount()==1,"full_inventory_does_not_debit_even_if_last_emerald_would_free_slot");
        var off=new ItemStack(StationAccess.TICKET);p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,off);
        check(Transactions.purchase(p,data).equals("recharged")&&data.state.wallets.balance(Ticket.id(off))==10,"offhand_ticket_can_recharge_with_full_inventory");
        var primary=new ItemStack(StationAccess.TICKET);Ticket.topUp(primary,data);p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,primary);p.getInventory().setItem(2,new ItemStack(Items.EMERALD,2));
        check(Transactions.purchase(p,data).equals("recharged")&&data.state.wallets.balance(Ticket.id(primary))==20&&data.state.wallets.balance(Ticket.id(off))==10,"mainhand_ticket_has_priority_over_offhand");
        data.state.wallets.balances.put(Ticket.id(primary),cn.quit5700.stationaccess.core.Wallets.MAX-5);int gems=p.getInventory().getItem(2).getCount();
        check(Transactions.purchase(p,data).equals("limit")&&p.getInventory().getItem(2).getCount()==gems,"credit_limit_does_not_debit_currency");
        p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.STONE));p.getInventory().setItem(1,new ItemStack(Items.EMERALD));p.getInventory().setItem(3,card);
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        var laterTicket=new ItemStack(StationAccess.TICKET);p.getInventory().setItem(9,laterTicket);
        var receipt=Transactions.purchaseDetailed(p,data);
        check(receipt.message().equals("recharged")&&receipt.ticketSlot()==3&&data.state.wallets.balance(Ticket.id(card))==30
                &&p.getInventory().getItem(2).isEmpty()&&Ticket.id(laterTicket).isEmpty(),"first_backpack_ticket_recharged_without_issuing_another");
        for(int i=0;i<36;i++)if(p.getInventory().getItem(i).isEmpty())p.getInventory().setItem(i,new ItemStack(Items.STONE));
        p.getInventory().setItem(1,new ItemStack(Items.EMERALD,2));
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,off);
        check(Transactions.purchase(p,data).equals("recharged")&&data.state.wallets.balance(Ticket.id(off))==20
                &&data.state.wallets.balance(Ticket.id(card))==30,"offhand_ticket_precedes_backpack");
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        check(Transactions.purchase(p,data).equals("recharged")&&data.state.wallets.balance(Ticket.id(card))==40
                &&p.getInventory().getItem(1).isEmpty(),"backpack_ticket_recharges_with_full_inventory");
        p.getInventory().setItem(1,new ItemStack(Items.EMERALD));
        p.getInventory().setItem(2,ItemStack.EMPTY);
        p.getInventory().setItem(3,new ItemStack(Items.STONE));
        p.getInventory().setItem(9,new ItemStack(Items.STONE));
        receipt=Transactions.purchaseDetailed(p,data);
        check(receipt.message().equals("purchased")&&receipt.ticketSlot()==2
                &&data.state.wallets.balance(Ticket.id(p.getInventory().getItem(receipt.ticketSlot())))==10,"new_ticket_receipt_reports_actual_slot");
        TicketHud.clear();var hud=new ProbePlayer(server);hud.getInventory().setItem(0,card);TicketHud.tick(hud,data);TicketHud.tick(hud,data);
        check(hud.overlayMessages==1,"held_ticket_displays_once_without_tick_spam");
        hud.getInventory().setItem(0,ItemStack.EMPTY);TicketHud.tick(hud,data);hud.getInventory().setItem(0,card);TicketHud.tick(hud,data);
        check(hud.overlayMessages==2,"switching_back_to_ticket_displays_again");Ticket.topUp(card,data);TicketHud.tick(hud,data);
        check(hud.overlayMessages==3,"ticket_balance_change_refreshes_overlay");
        var railway=new cn.quit5700.fabrictrain.data.RailwaySavedData();var auto=UUID.randomUUID();var manual=UUID.randomUUID();
        railway.ensureTrain(auto,true,9,cn.quit5700.fabrictrain.train.ControlMode.ROUTE_AUTOMATIC);railway.ensureTrain(manual,true,9,cn.quit5700.fabrictrain.train.ControlMode.MANUAL);railway.registerActiveTrain(auto,9);railway.registerActiveTrain(manual,9);
        check(railway.activeTrainCount(9)==2&&railway.activeAutomaticTrainCount(9)==1,"manual_train_cannot_occupy_automatic_quota");
    }

    private static void done(MinecraftServer server,boolean pass,String error){phase=99;try{var json=new JsonObject();json.addProperty("passed",pass);json.addProperty("count",CHECKS.size());json.add("checks",AccessData.GSON.toJsonTree(CHECKS));json.addProperty("error",error);Files.writeString(Path.of("station-access-"+MODE+"-checks.json"),json.toString(),StandardCharsets.UTF_8);}catch(Exception write){throw new IllegalStateException(write);}server.halt(false);}
}
