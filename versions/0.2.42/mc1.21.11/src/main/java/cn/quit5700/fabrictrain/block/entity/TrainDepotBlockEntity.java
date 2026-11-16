package cn.quit5700.fabrictrain.block.entity;

import cn.quit5700.fabrictrain.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import cn.quit5700.fabrictrain.depot.DepotTemplate;
import cn.quit5700.fabrictrain.train.TrainClass;
import cn.quit5700.fabrictrain.train.SpeedRules;
import cn.quit5700.fabrictrain.depot.DepotDispatchResult;

public final class TrainDepotBlockEntity extends BaseContainerBlockEntity {
    public static final int SIZE = 54;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private DepotTemplate template = DepotTemplate.defaultTemplate();
    private long selectedLineId;
    private TrainClass trainClass = TrainClass.PASSENGER;
    private int dispatchIntervalSeconds = 30;
    private int maximumTrains = 1;
    private final java.util.Set<java.util.UUID> pendingRecalls=new java.util.LinkedHashSet<>();
    public java.util.Set<java.util.UUID> pendingRecalls(){return pendingRecalls;}
    private double cargoSpeedKmh = SpeedRules.FREIGHT_LIMIT_KMH;
    private double passengerSpeedKmh = SpeedRules.PASSENGER_LIMIT_KMH;
    private double expressSpeedKmh = SpeedRules.EXPRESS_LIMIT_KMH;
    private boolean automaticDispatchEnabled;
    private boolean creativeAutomaticDispatch;
    private int lastAutomaticDispatchResult = -1;

    public TrainDepotBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRAIN_DEPOT, pos, state);
    }

    @Override public int getContainerSize() { return SIZE; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> value) { items = value; }
    @Override protected Component getDefaultName() { return Component.translatable("container.fabric_train_simulator.train_depot"); }
    @Override protected AbstractContainerMenu createMenu(int syncId, Inventory inventory) { return ChestMenu.sixRows(syncId, inventory, this); }
    public DepotTemplate template() { return template; }
    public void setTemplate(DepotTemplate value) { template = value; setChanged(); }
    public long selectedLineId() { return selectedLineId; }
    public void setSelectedLineId(long lineId) {
        long next = Math.max(0L, lineId);
        if (selectedLineId != next) {
            automaticDispatchEnabled = false;
            creativeAutomaticDispatch = false;
        }
        selectedLineId = next;
        setChanged();
    }
    public boolean automaticDispatchEnabled() { return automaticDispatchEnabled; }
    public void setAutomaticDispatchEnabled(boolean enabled) {
        automaticDispatchEnabled = enabled && selectedLineId > 0L;
        if (!automaticDispatchEnabled) creativeAutomaticDispatch = false;
        setChanged();
    }
    public boolean creativeAutomaticDispatch() {
        if (getLevel() instanceof net.minecraft.server.level.ServerLevel current) {
            var pos = getBlockPos();
            var device = cn.quit5700.fabrictrain.data.RailwayDataManager.state(current).find(
                    cn.quit5700.fabrictrain.data.RailwayDataManager.dimension(current), pos.getX(), pos.getY(), pos.getZ());
            if (device != null) return cn.quit5700.fabrictrain.depot.DispatchAuthorityService.isCreative(current, device.id());
        }
        return creativeAutomaticDispatch;
    }
    public void setCreativeAutomaticDispatch(boolean creative) {
        creativeAutomaticDispatch = automaticDispatchEnabled && creative;
        setChanged();
    }
    public int lastAutomaticDispatchResultOrdinal() { return lastAutomaticDispatchResult; }
    public void setLastAutomaticDispatchResult(DepotDispatchResult result) {
        lastAutomaticDispatchResult = result == null ? -1 : result.ordinal();
        setChanged();
    }
    public TrainClass trainClass() { return template.cars().contains(cn.quit5700.fabrictrain.train.CarType.CARGO) ? TrainClass.CARGO : trainClass; }
    public int dispatchIntervalSeconds() { return dispatchIntervalSeconds; }
    public int maximumTrains() { return maximumTrains; }
    public double speedLimitKmh(TrainClass value) { return switch(value){
        case CARGO -> cargoSpeedKmh;
        case PASSENGER -> passengerSpeedKmh;
        case EXPRESS -> expressSpeedKmh;
    }; }
    public void configure(long lineId, TrainClass value, int interval, int maximum) {
        configure(lineId,value,interval,maximum,cargoSpeedKmh,passengerSpeedKmh,expressSpeedKmh);
    }
    public void configure(long lineId, TrainClass value, int interval, int maximum,
                          double cargoSpeed,double passengerSpeed,double expressSpeed) {
        if (selectedLineId != Math.max(0L, lineId)) {
            automaticDispatchEnabled = false;
            creativeAutomaticDispatch = false;
        }
        selectedLineId = Math.max(0, lineId);
        trainClass = value == null ? TrainClass.PASSENGER : value;
        dispatchIntervalSeconds = Math.clamp(interval, 1, 3600);
        maximumTrains = Math.clamp(maximum, 0, 1000);
        cargoSpeedKmh=SpeedRules.sanitizeConfiguredLimit(cargoSpeed,SpeedRules.FREIGHT_LIMIT_KMH);
        passengerSpeedKmh=SpeedRules.sanitizeConfiguredLimit(passengerSpeed,SpeedRules.PASSENGER_LIMIT_KMH);
        expressSpeedKmh=SpeedRules.sanitizeConfiguredLimit(expressSpeed,SpeedRules.EXPRESS_LIMIT_KMH);
        setChanged();
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putString("TrainTemplate", template.encode());
        output.putLong("SelectedLineId", selectedLineId);
        output.putString("TrainClass", trainClass.name());
        output.putInt("DispatchIntervalSeconds", dispatchIntervalSeconds);
        output.putInt("MaximumTrains", maximumTrains);
        output.putString("PendingRecalls",pendingRecalls.stream().map(java.util.UUID::toString).collect(java.util.stream.Collectors.joining(",")));
        output.putDouble("CargoSpeedKmh",cargoSpeedKmh);
        output.putDouble("PassengerSpeedKmh",passengerSpeedKmh);
        output.putDouble("ExpressSpeedKmh",expressSpeedKmh);
        output.putBoolean("AutomaticDispatchEnabled", automaticDispatchEnabled);
        output.putBoolean("CreativeAutomaticDispatch", creativeAutomaticDispatch);
        output.putInt("LastAutomaticDispatchResult", lastAutomaticDispatchResult);
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        template = DepotTemplate.decode(input.getStringOr("TrainTemplate", DepotTemplate.defaultTemplate().encode()));
        pendingRecalls.clear();for(String id:(input.getStringOr("PendingRecalls", "")).split(","))if(!id.isEmpty())try{pendingRecalls.add(java.util.UUID.fromString(id));}catch(IllegalArgumentException ignored){}
        selectedLineId = Math.max(0, input.getLongOr("SelectedLineId", 0));
        try { trainClass = TrainClass.valueOf(input.getStringOr("TrainClass", "PASSENGER")); }
        catch (IllegalArgumentException ignored) { trainClass = TrainClass.PASSENGER; }
        dispatchIntervalSeconds = Math.clamp(input.getIntOr("DispatchIntervalSeconds", 30), 1, 3600);
        maximumTrains = Math.clamp(input.getIntOr("MaximumTrains", 1), 0, 1000);
        cargoSpeedKmh=SpeedRules.sanitizeConfiguredLimit(input.getDoubleOr("CargoSpeedKmh",SpeedRules.FREIGHT_LIMIT_KMH),SpeedRules.FREIGHT_LIMIT_KMH);
        passengerSpeedKmh=SpeedRules.sanitizeConfiguredLimit(input.getDoubleOr("PassengerSpeedKmh",SpeedRules.PASSENGER_LIMIT_KMH),SpeedRules.PASSENGER_LIMIT_KMH);
        expressSpeedKmh=SpeedRules.sanitizeConfiguredLimit(input.getDoubleOr("ExpressSpeedKmh",SpeedRules.EXPRESS_LIMIT_KMH),SpeedRules.EXPRESS_LIMIT_KMH);
        automaticDispatchEnabled=input.getBooleanOr("AutomaticDispatchEnabled", false) && selectedLineId > 0L;
        creativeAutomaticDispatch=input.getBooleanOr("CreativeAutomaticDispatch", false) && automaticDispatchEnabled;
        lastAutomaticDispatchResult=Math.clamp(input.getIntOr("LastAutomaticDispatchResult", -1), -1,
                DepotDispatchResult.values().length - 1);
    }
}
