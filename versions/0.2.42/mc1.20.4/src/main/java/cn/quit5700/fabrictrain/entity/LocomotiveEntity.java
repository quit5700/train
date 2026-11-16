package cn.quit5700.fabrictrain.entity;

import cn.quit5700.fabrictrain.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import cn.quit5700.fabrictrain.train.LinkedTrainCar;
import cn.quit5700.fabrictrain.train.TrainLink;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import cn.quit5700.fabrictrain.registry.ModBlocks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import cn.quit5700.fabrictrain.train.ConnectorInteractionService;
import cn.quit5700.fabrictrain.train.LogicallyControlledVehicle;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

public final class LocomotiveEntity extends Minecart implements LinkedTrainCar, LogicallyControlledVehicle {
    private static final EntityDataAccessor<Boolean> LOGICALLY_CONTROLLED =
            SynchedEntityData.defineId(LocomotiveEntity.class, EntityDataSerializers.BOOLEAN);
    private final TrainLink trainLink = new TrainLink();
    private net.minecraft.world.phys.Vec3 parkedPosition;
    private final cn.quit5700.fabrictrain.train.RailInterpolation railInterpolation=new cn.quit5700.fabrictrain.train.RailInterpolation();
    private Vec3 railNetworkTarget;private int railNetworkSteps;

    public LocomotiveEntity(EntityType<?> type, Level level) {
        super(type, level);
        trainLink.assign(getUUID(), getUUID(), 0);
    }

    @Override
    protected Item getDropItem() {
        return ModItems.LOCOMOTIVE;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.LOCOMOTIVE);
    }

    @Override public BlockState getDefaultDisplayBlockState() { return ModBlocks.LOCOMOTIVE_DISPLAY.defaultBlockState(); }
    @Override public int getDefaultDisplayOffset() { return 6; }

    @Override public TrainLink trainLink() { return trainLink; }
    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(LOGICALLY_CONTROLLED, false);
    }
    @Override public boolean fabricTrain$isLogicallyControlled() { return entityData.get(LOGICALLY_CONTROLLED); }
    @Override public void fabricTrain$setLogicallyControlled(boolean controlled) { entityData.set(LOGICALLY_CONTROLLED, controlled); }
    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        InteractionResult result = ConnectorInteractionService.rightClick(player, hand, this);
        return result != InteractionResult.PASS ? result : super.interact(player, hand);
    }
    @Override public void push(net.minecraft.world.entity.Entity entity) {
        if (fabricTrain$isLogicallyControlled()) return;
        if (entity instanceof CargoCarEntity cargo) {
            cargo.beginAuthorizedPush();
            try { super.push(entity); } finally { cargo.endAuthorizedPush(); }
        } else super.push(entity);
    }
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps) {
        super.lerpTo(x,y,z,yaw,pitch,steps);
        if(level().isClientSide()){railNetworkTarget=new Vec3(x,y,z);railNetworkSteps=steps+2;}
    }
    @Override public void tick(){
        Vec3 before = position();
        boolean controlled = fabricTrain$isLogicallyControlled();
        boolean clientSide = level().isClientSide();
        if(!clientSide && trainLink.parked()&&parkedPosition==null)parkedPosition=position();
        if(!clientSide && !trainLink.parked())parkedPosition=null;
        super.tick();
        if(clientSide&&railNetworkSteps>0&&railNetworkTarget!=null){
            if(controlled){Vec3 aligned=railInterpolation.step(level(),before,railNetworkTarget,railNetworkSteps);if(aligned!=null)setPos(aligned);}
            railNetworkSteps--;
        }
        if (LogicallyControlledVehicle.shouldRestorePosition(clientSide, controlled, false)) {
            setPos(before);
            setDeltaMovement(Vec3.ZERO);
        } else if(!clientSide && trainLink.parked()){
            setDeltaMovement(Vec3.ZERO);
            if(parkedPosition!=null)setPos(parkedPosition);
        }
    }
    @Override public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel serverLevel)
            cn.quit5700.fabrictrain.train.TrainControllerManager.noteVehicleRemoved(
                    serverLevel, trainLink.trainId(), getUUID(), reason);
        super.remove(reason);
    }
    @Override protected void readAdditionalSaveData(CompoundTag input) { super.readAdditionalSaveData(input); trainLink.load(input); }
    @Override protected void addAdditionalSaveData(CompoundTag output) { super.addAdditionalSaveData(output); trainLink.save(output); }
}


