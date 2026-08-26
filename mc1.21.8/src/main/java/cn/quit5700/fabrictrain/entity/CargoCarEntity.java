package cn.quit5700.fabrictrain.entity;

import cn.quit5700.fabrictrain.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.MinecartChest;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import cn.quit5700.fabrictrain.train.ConnectorInteractionService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import cn.quit5700.fabrictrain.train.LogicallyControlledVehicle;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

public final class CargoCarEntity extends MinecartChest implements LinkedTrainCar, LogicallyControlledVehicle {
    private static final EntityDataAccessor<Boolean> LOGICALLY_CONTROLLED =
            SynchedEntityData.defineId(CargoCarEntity.class, EntityDataSerializers.BOOLEAN);
    private final TrainLink trainLink = new TrainLink();
    private long destinationStationId;
    private boolean ownTick;
    private int authorizedPushDepth;
    private int placementBrakeTicks = 40;
    public CargoCarEntity(EntityType<? extends MinecartChest> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDropItem() {
        return ModItems.CARGO_CAR;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.CARGO_CAR);
    }

    @Override public BlockState getDefaultDisplayBlockState() { return ModBlocks.CARGO_DISPLAY.defaultBlockState(); }
    @Override public int getDefaultDisplayOffset() { return 6; }

    @Override public TrainLink trainLink() { return trainLink; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LOGICALLY_CONTROLLED, false);
    }
    @Override public boolean fabricTrain$isLogicallyControlled() { return entityData.get(LOGICALLY_CONTROLLED); }
    @Override public void fabricTrain$setLogicallyControlled(boolean controlled) { entityData.set(LOGICALLY_CONTROLLED, controlled); }
    public void releasePlacementBrake() { placementBrakeTicks = 0; }
    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        InteractionResult result = ConnectorInteractionService.rightClick(player, hand, this);
        return result != InteractionResult.PASS ? result : super.interact(player, hand);
    }
    @Override public void push(Entity entity) {
        if (placementBrakeTicks > 0 || fabricTrain$isLogicallyControlled()) return;
        if (entity instanceof PassengerCarEntity || entity instanceof LocomotiveEntity) super.push(entity);
    }
    public void beginAuthorizedPush() { authorizedPushDepth++; }
    public void endAuthorizedPush() { authorizedPushDepth = Math.max(0, authorizedPushDepth - 1); }
    private boolean movementAuthorized() { return ownTick || authorizedPushDepth > 0 || trainLink.hasLocomotive(); }
    @Override public void setDeltaMovement(Vec3 movement) {
        if (movementAuthorized()) super.setDeltaMovement(movement);
    }
    @Override public void push(double x, double y, double z) {
        if (movementAuthorized()) super.push(x, y, z);
    }
    @Override public void tick() {
        Vec3 before = position();
        boolean linked = fabricTrain$isLogicallyControlled();
        boolean placementBraked = placementBrakeTicks > 0;
        ownTick = true;
        try { super.tick(); } finally { ownTick = false; }
        if (placementBraked) placementBrakeTicks--;
        if (LogicallyControlledVehicle.shouldRestorePosition(
                level().isClientSide(), linked, placementBraked)) {
            beginAuthorizedPush();
            try {
                setPos(before);
                setDeltaMovement(Vec3.ZERO);
            } finally { endAuthorizedPush(); }
        }
    }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && player.getAbilities().instabuild) {
            destroy(level, getDropItem());
            return true;
        }
        return super.hurtServer(level, source, amount);
    }
    @Override public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel serverLevel)
            cn.quit5700.fabrictrain.train.TrainControllerManager.noteVehicleRemoved(
                    serverLevel, trainLink.trainId(), getUUID(), reason);
        super.remove(reason);
    }
    public long destinationStationId() { return destinationStationId; }
    public void setDestinationStationId(long value) { destinationStationId = Math.max(0L, value); }
    @Override protected void readAdditionalSaveData(ValueInput input) { super.readAdditionalSaveData(input); trainLink.load(input); destinationStationId = Math.max(0L, input.getLongOr("DestinationStationId", 0L)); placementBrakeTicks = 0; }
    @Override protected void addAdditionalSaveData(ValueOutput output) { super.addAdditionalSaveData(output); trainLink.save(output); output.putLong("DestinationStationId", destinationStationId); }
}
