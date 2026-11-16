package cn.quit5700.fabrictrain.depot;

import cn.quit5700.fabrictrain.block.entity.TrainDepotBlockEntity;
import cn.quit5700.fabrictrain.entity.CargoCarEntity;
import cn.quit5700.fabrictrain.entity.LocomotiveEntity;
import cn.quit5700.fabrictrain.entity.PassengerCarEntity;
import cn.quit5700.fabrictrain.registry.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import cn.quit5700.fabrictrain.data.RailwayDataManager;
import net.minecraft.world.Container;
import cn.quit5700.fabrictrain.cargo.CargoTransferService;

import java.util.ArrayList;
import java.util.List;

public final class DepotRecoveryService {
    private DepotRecoveryService() {}

    public static boolean recover(List<Entity> cars, TrainDepotBlockEntity depot) {
        if(cars.isEmpty()||cars.stream().allMatch(Entity::isRemoved))return true;
        var head=cars.stream().filter(LocomotiveEntity.class::isInstance).map(LocomotiveEntity.class::cast).findFirst().orElse(null);
        if(head!=null&&head.isRemoved())return false;
        if(head!=null&&head.level() instanceof ServerLevel world
                &&cn.quit5700.fabrictrain.offline.InfrastructureService.data(world.getServer()).retired.contains(head.trainLink().trainId()))return false;
        var manifest=head==null?null:RailwayDataManager.state((ServerLevel)head.level()).trainManifest(head.trainLink().trainId());
        if(manifest!=null&&cars.size()!=manifest.totalVehicles())return false;
        List<Container> containers = new ArrayList<>(); containers.add(depot);
        if (depot.getLevel() instanceof ServerLevel level)
            containers.addAll(CargoTransferService.adjacentContainers(level, depot.getBlockPos()));
        List<ItemStack> simulated = new ArrayList<>();
        for (Container container : containers) for (int slot=0;slot<container.getContainerSize();slot++)
            simulated.add(container.getItem(slot).copy());
        List<ItemStack> returning = new ArrayList<>();
        for (Entity entity : cars) {
            if (entity instanceof CargoCarEntity cargo) {
                for (int slot = 0; slot < cargo.getContainerSize(); slot++) {
                    if (!cargo.getItem(slot).isEmpty()) returning.add(cargo.getItem(slot).copy());
                }
                returning.add(new ItemStack(ModItems.CARGO_CAR));
            } else if (entity instanceof PassengerCarEntity) {
                returning.add(new ItemStack(ModItems.PASSENGER_CAR));
            } else if (entity instanceof LocomotiveEntity) {
                returning.add(new ItemStack(ModItems.LOCOMOTIVE));
            }
        }
        java.util.List<ItemStack> overflow=new ArrayList<>();for(ItemStack stack:returning){insert(simulated,stack);if(!stack.isEmpty())overflow.add(stack.copy());}
        if(!(depot.getLevel() instanceof ServerLevel dropLevel)||!dropOverflow(dropLevel,depot,overflow))return false;
        int flat=0;for(Container container:containers){for(int slot=0;slot<container.getContainerSize();slot++)container.setItem(slot,simulated.get(flat++));container.setChanged();}
        cars.stream().filter(LocomotiveEntity.class::isInstance).map(LocomotiveEntity.class::cast)
                .findFirst().ifPresent(locomotive -> {
                    if (locomotive.level() instanceof ServerLevel level) {
                        cn.quit5700.fabrictrain.train.TrainControllerManager.retireTrainResources(
                                level, locomotive.trainLink().trainId());
                    }
                });
        cars.forEach(car->{if(car instanceof CargoCarEntity cargo)cargo.clearContent();for(Entity rider:new ArrayList<>(car.getPassengers())){car.ejectPassengers();safeDismount(rider,depot);}car.discard();});
        return true;
    }

    public static boolean recoverItems(ServerLevel level, TrainDepotBlockEntity depot, List<ItemStack> returning) {
        List<Container> containers = new ArrayList<>(); containers.add(depot);
        containers.addAll(CargoTransferService.adjacentContainers(level, depot.getBlockPos()));
        List<ItemStack> simulated = new ArrayList<>();
        for (Container container : containers) for (int slot = 0; slot < container.getContainerSize(); slot++)
            simulated.add(container.getItem(slot).copy());
        java.util.List<ItemStack> overflow=new ArrayList<>();for(ItemStack stack:returning){ItemStack copy=stack.copy();insert(simulated,copy);if(!copy.isEmpty())overflow.add(copy);}
        if(!dropOverflow(level,depot,overflow))return false;
        int flat = 0;
        for (Container container : containers) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) container.setItem(slot, simulated.get(flat++));
            container.setChanged();
        }
        return true;
    }


    private static void safeDismount(Entity rider,TrainDepotBlockEntity depot){
        var pos=depot.getBlockPos();
        for(int dy=1;dy<=8;dy++)for(int radius=0;radius<=3;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            double x=pos.getX()+dx+0.5,y=pos.getY()+dy+0.1,z=pos.getZ()+dz+0.5;
            var delta=new net.minecraft.world.phys.Vec3(x-rider.getX(),y-rider.getY(),z-rider.getZ());
            if(depot.getLevel().noCollision(rider,rider.getBoundingBox().move(delta))){rider.teleportTo(x,y,z);return;}
        }
    }

    private static boolean dropOverflow(ServerLevel level,TrainDepotBlockEntity depot,List<ItemStack> overflow){
        var spawned=new ArrayList<net.minecraft.world.entity.item.ItemEntity>();
        for(ItemStack stack:overflow)while(!stack.isEmpty()){
            var portion=stack.split(Math.min(stack.getMaxStackSize(),stack.getCount()));var pos=depot.getBlockPos();
            var drop=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+0.5,pos.getY()+1.1,pos.getZ()+0.5,portion);
            drop.setDefaultPickUpDelay();
            if(!level.addFreshEntity(drop)){spawned.forEach(Entity::discard);return false;}spawned.add(drop);
        }return true;
    }

    private static boolean insert(List<ItemStack> inventory, ItemStack source) {
        for (ItemStack target : inventory) {
            if (source.isEmpty()) return true;
            if (target.isEmpty() || !ItemStack.isSameItemSameTags(source, target)) continue;
            int room = target.getMaxStackSize() - target.getCount();
            int moved = Math.min(room, source.getCount());
            target.grow(moved);
            source.shrink(moved);
        }
        for (int slot = 0; slot < inventory.size() && !source.isEmpty(); slot++) {
            if (!inventory.get(slot).isEmpty()) continue;
            int moved = Math.min(source.getMaxStackSize(), source.getCount());
            inventory.set(slot, source.copyWithCount(moved));
            source.shrink(moved);
        }
        return source.isEmpty();
    }
}
