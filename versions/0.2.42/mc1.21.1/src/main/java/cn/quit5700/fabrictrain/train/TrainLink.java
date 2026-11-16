package cn.quit5700.fabrictrain.train;


import cn.quit5700.fabrictrain.compat.NbtCompat;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;

import java.util.Optional;
import java.util.UUID;

public final class TrainLink {
    private UUID trainId;
    private UUID locomotiveId;
    private int carIndex;
    private long lineId;
    private TrainClass trainClass = TrainClass.PASSENGER;
    private boolean loadedFromSave;
    private UUID savedSessionId = TrainSession.current();
    private int nextNodeIndex = 1;
    private long lastDeviceId;
    private long lockedSwitchId;
    private boolean automaticRunning;
    private long destinationStationId;
    private boolean parked=true;
    private final ManualStationControl manualStationControl=new ManualStationControl();
    public ManualStationControl manualStationControl(){return manualStationControl;}
    private boolean locomotivePresent;
    private boolean depotGenerated;
    private ControlMode controlMode = ControlMode.MANUAL;
    private double configuredSpeedLimitKmh;
    private int formationForwardOrdinal = -1;
    private long originDepotNumber;
    private long registryTrainNumber;
    private int vehicleNumber = 1;

    public TrainLink() {
        UUID standalone = UUID.randomUUID();
        trainId = standalone;
        locomotiveId = standalone;
    }

    public void assign(UUID trainId, UUID locomotiveId, int carIndex) {
        assign(trainId, locomotiveId, carIndex, lineId);
    }

    public void assign(UUID trainId, UUID locomotiveId, int carIndex, long lineId) {
        if (carIndex < 0) throw new IllegalArgumentException("Car index cannot be negative");
        if (lineId < 0) throw new IllegalArgumentException("Line id cannot be negative");
        this.trainId = trainId;
        this.locomotiveId = locomotiveId;
        this.carIndex = carIndex;
        this.lineId = lineId;
        this.locomotivePresent = true;
    }

    public void assignWithoutLocomotive(UUID trainId, int carIndex) {
        if (carIndex < 0) throw new IllegalArgumentException("Car index cannot be negative");
        this.trainId = trainId;
        this.locomotiveId = new UUID(0L, 0L);
        this.carIndex = carIndex;
        this.lineId = 0L;
        this.locomotivePresent = false;
        this.automaticRunning = false;
        this.formationForwardOrdinal = -1;
    }

    public UUID trainId() { return trainId; }
    public UUID locomotiveId() { return locomotiveId; }
    public int carIndex() { return carIndex; }
    public long originDepotNumber() { return originDepotNumber; }
    public long registryTrainNumber() { return registryTrainNumber; }
    public int vehicleNumber() { return vehicleNumber; }
    public void setRegistryAddress(long depotNumber, long trainNumber, int vehicleNumber) {
        this.originDepotNumber = Math.max(0L, depotNumber);
        this.registryTrainNumber = Math.max(0L, trainNumber);
        this.vehicleNumber = Math.max(1, vehicleNumber);
    }
    public long lineId() { return lineId; }
    public void detachFromLine() {
        lineId = 0L;
        automaticRunning = false;
        nextNodeIndex = 1;
        lastDeviceId = 0L;
        lockedSwitchId = 0L;
    }
    public TrainClass trainClass() { return trainClass; }
    public void setTrainClass(TrainClass requested) { trainClass = requested == null ? TrainClass.PASSENGER : requested; }
    public boolean loadedFromSave() { return loadedFromSave; }
    public void markStartupRecovered() { loadedFromSave = false; }
    public int nextNodeIndex() { return nextNodeIndex; }
    public long lastDeviceId() { return lastDeviceId; }
    public long lockedSwitchId() { return lockedSwitchId; }
    public void updateRouteProgress(int nextNodeIndex, long lastDeviceId, long lockedSwitchId) {
        this.nextNodeIndex = Math.max(0, nextNodeIndex);
        this.lastDeviceId = Math.max(0L, lastDeviceId);
        this.lockedSwitchId = Math.max(0L, lockedSwitchId);
    }
    public boolean automaticRunning() { return automaticRunning; }
    public void setAutomaticRunning(boolean value) { automaticRunning = value;if(value)parked=false; }
    public boolean parked(){return parked;}
    public void setParked(boolean value){parked=value;if(value)automaticRunning=false;}
    public boolean hasLocomotive(){return locomotivePresent;}
    public boolean depotGenerated(){return depotGenerated;}
    public void setDepotGenerated(boolean value){depotGenerated=value;}
    public ControlMode controlMode(){return controlMode;}
    public void setControlMode(ControlMode value){controlMode=value==null?ControlMode.MANUAL:value;}
    public double configuredSpeedLimitKmh(){return configuredSpeedLimitKmh;}
    public void setConfiguredSpeedLimitKmh(double value){configuredSpeedLimitKmh=Double.isFinite(value)&&value>0?value:0D;}
    public Optional<Direction> formationForward(){
        if(formationForwardOrdinal<0||formationForwardOrdinal>=Direction.values().length)return Optional.empty();
        Direction direction=Direction.values()[formationForwardOrdinal];
        return direction.getAxis()==Direction.Axis.Y?Optional.empty():Optional.of(direction);
    }
    public void setFormationForward(Direction direction){
        formationForwardOrdinal=direction==null||direction.getAxis()==Direction.Axis.Y?-1:direction.ordinal();
    }
    public void clearFormationForward(){formationForwardOrdinal=-1;}
    public boolean standalone(){return !locomotivePresent&&carIndex==0;}
    public long destinationStationId(){return destinationStationId;}
    public void setDestinationStationId(long value){destinationStationId=Math.max(0,value);}
    public void detach(UUID entityId){trainId=entityId;locomotiveId=new UUID(0L,0L);carIndex=0;lineId=0;locomotivePresent=false;automaticRunning=false;nextNodeIndex=1;lastDeviceId=0;lockedSwitchId=0;destinationStationId=0;formationForwardOrdinal=-1;originDepotNumber=0L;registryTrainNumber=0L;vehicleNumber=1;}

    public void load(CompoundTag input) {
        trainId = parse(NbtCompat.getString(input, "TrainId", trainId.toString()), trainId);
        locomotiveId = parse(NbtCompat.getString(input, "LocomotiveId", locomotiveId.toString()), locomotiveId);
        carIndex = Math.max(0, NbtCompat.getInt(input, "CarIndex", 0));
        lineId = Math.max(0L, NbtCompat.getLong(input, "LineId", 0L));
        try { trainClass = TrainClass.valueOf(NbtCompat.getString(input, "TrainClass", TrainClass.PASSENGER.name())); }
        catch (IllegalArgumentException ignored) { trainClass = TrainClass.PASSENGER; }
        savedSessionId = parse(NbtCompat.getString(input, "ServerSessionId", new UUID(0L, 0L).toString()), new UUID(0L, 0L));
        loadedFromSave = !savedSessionId.equals(TrainSession.current());
        nextNodeIndex = Math.max(0, NbtCompat.getInt(input, "NextNodeIndex", 1));
        lastDeviceId = Math.max(0L, NbtCompat.getLong(input, "LastDeviceId", 0L));
        lockedSwitchId = Math.max(0L, NbtCompat.getLong(input, "LockedSwitchId", 0L));
        automaticRunning = NbtCompat.getBoolean(input, "AutomaticRunning", false);
        destinationStationId=Math.max(0,NbtCompat.getLong(input, "DestinationStationId",0L));
        parked=NbtCompat.getBoolean(input, "Parked",false);
        manualStationControl.restore(NbtCompat.getBoolean(input, "ManualStationMode",false),NbtCompat.getBoolean(input, "ManualStationWaiting",false));
        locomotivePresent=NbtCompat.getBoolean(input, "LocomotivePresent", !locomotiveId.equals(new UUID(0L,0L)));
        depotGenerated=NbtCompat.getBoolean(input, "DepotGenerated",false);
        try { controlMode=ControlMode.valueOf(NbtCompat.getString(input, "ControlMode",
                depotGenerated&&lineId>0?ControlMode.ROUTE_AUTOMATIC.name():ControlMode.MANUAL.name())); }
        catch(IllegalArgumentException ignored){controlMode=depotGenerated&&lineId>0?ControlMode.ROUTE_AUTOMATIC:ControlMode.MANUAL;}
        setConfiguredSpeedLimitKmh(NbtCompat.getDouble(input, "ConfiguredSpeedLimitKmh",0D));
        formationForwardOrdinal=NbtCompat.getInt(input, "FormationForwardOrdinal",-1);
        originDepotNumber=Math.max(0L,NbtCompat.getLong(input, "OriginDepotNumber",0L));
        registryTrainNumber=Math.max(0L,NbtCompat.getLong(input, "RegistryTrainNumber",0L));
        vehicleNumber=Math.max(1,NbtCompat.getInt(input, "VehicleNumber",carIndex+1));
        if(formationForward().isEmpty())formationForwardOrdinal=-1;
    }

    public void save(CompoundTag output) {
        output.putString("TrainId", trainId.toString());
        output.putString("LocomotiveId", locomotiveId.toString());
        output.putInt("CarIndex", carIndex);
        output.putLong("LineId", lineId);
        output.putString("TrainClass", trainClass.name());
        output.putString("ServerSessionId", TrainSession.current().toString());
        output.putInt("NextNodeIndex", nextNodeIndex);
        output.putLong("LastDeviceId", lastDeviceId);
        output.putLong("LockedSwitchId", lockedSwitchId);
        output.putBoolean("AutomaticRunning", automaticRunning);
        output.putLong("DestinationStationId",destinationStationId);
        output.putBoolean("Parked",parked);
        output.putBoolean("ManualStationMode",manualStationControl.enabled());
        output.putBoolean("ManualStationWaiting",manualStationControl.waiting());
        output.putBoolean("LocomotivePresent",locomotivePresent);
        output.putBoolean("DepotGenerated",depotGenerated);
        output.putString("ControlMode",controlMode.name());
        output.putDouble("ConfiguredSpeedLimitKmh",configuredSpeedLimitKmh);
        output.putInt("FormationForwardOrdinal",formationForwardOrdinal);
        output.putLong("OriginDepotNumber",originDepotNumber);
        output.putLong("RegistryTrainNumber",registryTrainNumber);
        output.putInt("VehicleNumber",vehicleNumber);
    }

    private static UUID parse(String value, UUID fallback) {
        try { return UUID.fromString(value); } catch (IllegalArgumentException ignored) { return fallback; }
    }
}
