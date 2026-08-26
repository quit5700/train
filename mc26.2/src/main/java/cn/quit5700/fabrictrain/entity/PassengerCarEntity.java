package cn.quit5700.fabrictrain.entity;

import cn.quit5700.fabrictrain.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import cn.quit5700.fabrictrain.train.LinkedTrainCar;
import cn.quit5700.fabrictrain.train.TrainLink;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.state.BlockState;
import cn.quit5700.fabrictrain.registry.ModBlocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import cn.quit5700.fabrictrain.train.ConnectorInteractionService;
import cn.quit5700.fabrictrain.train.LogicallyControlledVehicle;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;

public final class PassengerCarEntity extends Minecart implements LinkedTrainCar, LogicallyControlledVehicle {
    private static final EntityDataAccessor<Boolean> LOGICALLY_CONTROLLED =
            SynchedEntityData.defineId(PassengerCarEntity.class, EntityDataSerializers.BOOLEAN);
    private final TrainLink trainLink = new TrainLink();
    private int placementBrakeTicks = 40;
    public PassengerCarEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDropItem() {
        return ModItems.PASSENGER_CAR;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.PASSENGER_CAR);
    }

    @Override public BlockState getDefaultDisplayBlockState() { return ModBlocks.PASSENGER_DISPLAY.defaultBlockState(); }
    @Override public int getDefaultDisplayOffset() { return 6; }

    @Override public TrainLink trainLink() { return trainLink; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LOGICALLY_CONTROLLED, false);
    }
    @Override public boolean fabricTrain$isLogicallyControlled() { return entityData.get(LOGICALLY_CONTROLLED); }
    @Override public void fabricTrain$setLogicallyControlled(boolean controlled) { entityData.set(LOGICALLY_CONTROLLED, controlled); }
    public void releasePlacementBrake() { placementBrakeTicks = 0; }
    @Override public InteractionResult interact(Player player, InteractionHand hand, Vec3 hit) {
        InteractionResult result = ConnectorInteractionService.rightClick(player, hand, this);
        return result != InteractionResult.PASS ? result : super.interact(player, hand, hit);
    }
    @Override public void push(Entity entity) {
        if (placementBrakeTicks > 0 || fabricTrain$isLogicallyControlled()) return;
        if (entity instanceof CargoCarEntity cargo) {
            cargo.beginAuthorizedPush();
            try { super.push(entity); } finally { cargo.endAuthorizedPush(); }
        } else super.push(entity);
    }
    @Override public void tick() {
        Vec3 before = position();
        boolean linked = fabricTrain$isLogicallyControlled();
        boolean placementBraked = placementBrakeTicks > 0;
        super.tick();
        if (placementBraked) placementBrakeTicks--;
        if (LogicallyControlledVehicle.shouldRestorePosition(
                level().isClientSide(), linked, placementBraked)) {
            setPos(before);
            setDeltaMovement(Vec3.ZERO);
        }
    }
    @Override public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel serverLevel)
            cn.quit5700.fabrictrain.train.TrainControllerManager.noteVehicleRemoved(
                    serverLevel, trainLink.trainId(), getUUID(), reason);
        super.remove(reason);
    }
    @Override protected void readAdditionalSaveData(ValueInput input) { super.readAdditionalSaveData(input); trainLink.load(input); placementBrakeTicks = 0; }
    @Override protected void addAdditionalSaveData(ValueOutput output) { super.addAdditionalSaveData(output); trainLink.save(output); }
}
