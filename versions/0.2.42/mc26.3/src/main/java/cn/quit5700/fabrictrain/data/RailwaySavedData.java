package cn.quit5700.fabrictrain.data;

import cn.quit5700.fabrictrain.FabricTrainMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import cn.quit5700.fabrictrain.train.TrainClass;
import cn.quit5700.fabrictrain.train.SpeedRules;
import cn.quit5700.fabrictrain.train.ControlMode;

public final class RailwaySavedData extends SavedData {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final Codec<RailwaySavedData> CODEC = Codec.PASSTHROUGH.xmap(
            RailwaySavedData::fromDynamic, RailwaySavedData::toDynamic);
    public static final SavedDataType<RailwaySavedData> TYPE = new SavedDataType<>(
            FabricTrainMod.id("railway_data"), RailwaySavedData::new, CODEC, null);

    private final Map<Long, RailwayDevice> devices = new LinkedHashMap<>();
    private final Map<Long, RailwayLine> lines = new LinkedHashMap<>();
    private final Map<UUID, Long> activeTrains = new LinkedHashMap<>();
    private final Map<UUID, TrainRecord> trainRegistry = new LinkedHashMap<>();
    private static final int STATION_HISTORY_TEXT_LIMIT = 192;
    private final Map<UUID, String> stationHistory = new LinkedHashMap<>();
    private final Map<UUID, TrainManifest> trainManifests = new LinkedHashMap<>();
    private final java.util.Set<UUID> pendingTrainDeletions = new java.util.LinkedHashSet<>();
    private long nextDeviceId = 1L;
    private long nextLineId = 1L;
    private long nextFreightYardNumber = 1L;
    private long nextStationNumber = 1L;
    private long nextSwitchNumber = 1L;
    private long nextDepotNumber = 1L;
    private long nextTrainNumber = 1L;
    private double freightSpeedLimitKmh = SpeedRules.FREIGHT_LIMIT_KMH;
    private double passengerSpeedLimitKmh = SpeedRules.PASSENGER_LIMIT_KMH;
    private double expressSpeedLimitKmh = SpeedRules.EXPRESS_LIMIT_KMH;

    public synchronized RailwayDevice find(String dimension, int x, int y, int z) {
        return devices.values().stream().filter(device ->
                device.dimension().equals(dimension) && device.x() == x && device.y() == y && device.z() == z)
                .findFirst().orElse(null);
    }

    public synchronized RailwayDevice register(DeviceType type, String dimension, int x, int y, int z) {
        RailwayDevice existing = find(dimension, x, y, z);
        if (existing != null) {
            return existing;
        }
        long id = nextDeviceId++;
        RailwayDevice device = new RailwayDevice(id, type, dimension, x, y, z,
                defaultName(type, nextDefaultNumber(type)), 30, 1);
        devices.put(id, device);
        setDirty();
        return device;
    }

    public synchronized void remove(String dimension, int x, int y, int z) {
        if (devices.values().removeIf(device -> device.dimension().equals(dimension)
                && device.x() == x && device.y() == y && device.z() == z)) {
            setDirty();
        }
    }

    public synchronized Map<Long, RailwayDevice> devices() {
        return Map.copyOf(devices);
    }

    public synchronized RailwayDevice device(long id) {
        return devices.get(id);
    }

    public synchronized RailwayDevice updateDevice(long id, String name, int dwellSeconds, int switchShapeOrdinal) {
        RailwayDevice device = devices.get(id);
        if (device == null) return null;
        String safeName = name == null || name.isBlank() ? device.name()
                : name.strip().substring(0, Math.min(64, name.strip().length()));
        RailwayDevice updated = new RailwayDevice(device.id(), device.type(), device.dimension(), device.x(), device.y(),
                device.z(), safeName, Math.clamp(dwellSeconds, 0, 3600), device.cargoDwellSeconds(), Math.max(0, switchShapeOrdinal), device.switchEntryOrdinal(),
                device.announcement(), device.manualUnload());
        devices.put(id, updated);
        setDirty();
        return updated;
    }

    public synchronized RailwayDevice updateDevice(long id, String name, int dwellSeconds, int switchShapeOrdinal, String announcement) {
        RailwayDevice updated=updateDevice(id,name,dwellSeconds,switchShapeOrdinal);
        if(updated==null)return null;
        String safe=announcement==null?"":announcement.strip().substring(0,Math.min(128,announcement.strip().length()));
        updated=new RailwayDevice(updated.id(),updated.type(),updated.dimension(),updated.x(),updated.y(),updated.z(),updated.name(),updated.dwellSeconds(),updated.cargoDwellSeconds(),updated.switchShapeOrdinal(),updated.switchEntryOrdinal(),safe,updated.manualUnload());
        devices.put(id,updated);setDirty();return updated;
    }

    public synchronized RailwayDevice updateManualUnload(long id, boolean manualUnload) {
        RailwayDevice device = devices.get(id);
        if (device == null) return null;
        RailwayDevice updated = new RailwayDevice(device.id(), device.type(), device.dimension(), device.x(), device.y(),
                device.z(), device.name(), device.dwellSeconds(), device.cargoDwellSeconds(), device.switchShapeOrdinal(), device.switchEntryOrdinal(), device.announcement(), manualUnload);
        devices.put(id, updated); setDirty(); return updated;
    }

    public synchronized RailwayDevice updateCargoDwell(long id, int cargoDwellSeconds) {
        RailwayDevice device = devices.get(id);
        if (device == null) return null;
        RailwayDevice updated = new RailwayDevice(device.id(), device.type(), device.dimension(), device.x(), device.y(),
                device.z(), device.name(), device.dwellSeconds(), Math.clamp(cargoDwellSeconds, 0, 3600),
                device.switchShapeOrdinal(), device.switchEntryOrdinal(), device.announcement(), device.manualUnload());
        devices.put(id, updated); setDirty(); return updated;
    }

    public synchronized RailwayDevice updateSwitchEntry(long id, int switchEntryOrdinal) {
        RailwayDevice device = devices.get(id);
        if (device == null || device.type() != DeviceType.SWITCH) return device;
        RailwayDevice updated = new RailwayDevice(device.id(), device.type(), device.dimension(), device.x(), device.y(),
                device.z(), device.name(), device.dwellSeconds(), device.cargoDwellSeconds(),
                device.switchShapeOrdinal(), switchEntryOrdinal, device.announcement(), device.manualUnload());
        devices.put(id, updated); setDirty(); return updated;
    }

    public synchronized RailwayLine line(long id) {
        return lines.get(id);
    }

    public synchronized Map<Long, RailwayLine> lines() {
        return Map.copyOf(lines);
    }

    public synchronized void registerActiveTrain(UUID trainId, long lineId) {
        Long previous = activeTrains.put(trainId, lineId);
        if (previous == null || previous != lineId) setDirty();
    }

    public synchronized void unregisterActiveTrain(UUID trainId) {
        if (activeTrains.remove(trainId) != null) setDirty();
    }

    public synchronized long activeAutomaticTrainCount(long lineId) {
        return activeTrains.entrySet().stream().filter(e -> e.getValue()==lineId)
            .filter(e -> trainRegistry.containsKey(e.getKey())&&trainRegistry.get(e.getKey()).controlMode()==ControlMode.ROUTE_AUTOMATIC).count();
    }
    public synchronized long activeTrainCount(long lineId) {
        return activeTrains.values().stream().filter(value -> value == lineId).count();
    }

    public synchronized TrainRecord ensureTrain(UUID trainId, boolean depotGenerated, long lineId) {
        return ensureTrain(trainId, depotGenerated, lineId,
                depotGenerated && lineId > 0L ? ControlMode.ROUTE_AUTOMATIC : ControlMode.MANUAL);
    }

    public synchronized TrainRecord ensureTrain(UUID trainId, boolean depotGenerated, long lineId,
                                                ControlMode controlMode) {
        TrainRecord existing = trainRegistry.get(trainId);
        ControlMode safeMode = controlMode == null ? ControlMode.MANUAL : controlMode;
        if (existing == null) {
            existing = new TrainRecord(trainId, nextTrainNumber++, depotGenerated, Math.max(0L, lineId), safeMode,
                    pendingTrainDeletions.contains(trainId) ? TrainPresence.DESTROYED : TrainPresence.LOADED);
            trainRegistry.put(trainId, existing);
            setDirty();
        } else if (pendingTrainDeletions.contains(trainId)) {
            if (existing.present()) {
                existing = new TrainRecord(existing.trainId(), existing.number(), existing.depotGenerated(),
                        existing.lineId(), existing.controlMode(), TrainPresence.DESTROYED);
                trainRegistry.put(trainId, existing);
                setDirty();
            }
            return existing;
        } else if ((depotGenerated && !existing.depotGenerated()) || existing.lineId() != lineId
                || existing.controlMode() != safeMode || !existing.present()) {
            existing = new TrainRecord(trainId, existing.number(),
                    existing.depotGenerated() || depotGenerated, Math.max(0L, lineId), safeMode,
                    TrainPresence.LOADED);
            trainRegistry.put(trainId, existing);
            setDirty();
        }
        return existing;
    }

    public synchronized TrainRecord trainRecord(UUID trainId) { return trainRegistry.get(trainId); }
    public synchronized Map<UUID, TrainRecord> trainRecords() { return Map.copyOf(trainRegistry); }
    public synchronized Map<UUID, TrainRecord> presentTrainRecords() {
        LinkedHashMap<UUID, TrainRecord> present = new LinkedHashMap<>();
        trainRegistry.forEach((id, record) -> {
            if (record.present() && hasRegisteredMembers(id)) present.put(id, record);
        });
        return Map.copyOf(present);
    }
    public synchronized Map<UUID, TrainRecord> presentAutomaticTrainRecords(long lineId) {
        if (lineId <= 0L) return Map.of();
        LinkedHashMap<UUID, TrainRecord> present = new LinkedHashMap<>();
        trainRegistry.forEach((id, record) -> {
            if (record.present() && hasRegisteredMembers(id) && record.lineId() == lineId
                    && record.controlMode() == ControlMode.ROUTE_AUTOMATIC) {
                present.put(id, record);
            }
        });
        return Map.copyOf(present);
    }
    public synchronized Map<UUID, TrainRecord> presentDepotTrainRecords(long lineId) {
        if (lineId <= 0L) return Map.of();
        LinkedHashMap<UUID, TrainRecord> present = new LinkedHashMap<>();
        trainRegistry.forEach((id, record) -> {
            if (!record.present() || !hasRegisteredMembers(id)) return;
            if (record.controlMode() == ControlMode.MANUAL
                    || record.controlMode() == ControlMode.ROUTE_AUTOMATIC && record.lineId() == lineId)
                present.put(id, record);
        });
        return Map.copyOf(present);
    }
    public synchronized Map<UUID,TrainRecord> presentTrainsFromDepot(long depotId){
        LinkedHashMap<UUID,TrainRecord> result=new LinkedHashMap<>();
        if(depotId<=0)return result;
        trainRegistry.forEach((id,record)->{
            TrainManifest manifest=trainManifests.get(id);
            if(record.present()&&manifest!=null&&manifest.totalVehicles()>0&&manifest.originDepotNumber()==depotId)result.put(id,record);
        });
        return Map.copyOf(result);
    }

    public synchronized TrainManifest trainManifest(UUID trainId) { return trainManifests.get(trainId); }
    public synchronized void replaceTrainManifest(UUID trainId, long originDepotNumber,
                                                  java.util.List<TrainMember> members) {
        if (trainId == null || pendingTrainDeletions.contains(trainId)) return;
        TrainManifest previous = trainManifests.get(trainId);
        long origin = originDepotNumber > 0L ? originDepotNumber
                : previous == null ? 0L : previous.originDepotNumber();
        TrainManifest manifest = new TrainManifest(origin, members);
        if (manifest.totalVehicles() == 0) trainManifests.remove(trainId);
        else trainManifests.put(trainId, manifest);
        setDirty();
    }
    public synchronized void registerTrainMember(UUID trainId, long originDepotNumber, TrainMember member) {
        if (trainId == null || member == null || pendingTrainDeletions.contains(trainId)) return;
        TrainManifest current = trainManifests.get(trainId);
        java.util.ArrayList<TrainMember> members = new java.util.ArrayList<>(
                current == null ? java.util.List.of() : current.members());
        members.removeIf(value -> value.entityId().equals(member.entityId()));
        members.add(member);
        replaceTrainManifest(trainId, originDepotNumber, members);
    }
    public synchronized MemberRemovalResult unregisterTrainMember(UUID trainId, UUID entityId) {
        TrainManifest current = trainManifests.get(trainId);
        if (current == null || entityId == null || !current.contains(entityId)) return MemberRemovalResult.UNKNOWN_MEMBER;
        TrainManifest remaining = current.without(entityId);
        if (remaining.totalVehicles() > 0) {
            trainManifests.put(trainId, remaining);
            setDirty();
            return MemberRemovalResult.MEMBERS_REMAIN;
        }
        trainManifests.remove(trainId);
        retireTrain(trainId);
        return MemberRemovalResult.TRAIN_EMPTY;
    }
    public synchronized boolean hasRegisteredMembers(UUID trainId) {
        TrainManifest manifest = trainManifests.get(trainId);
        return manifest != null && manifest.totalVehicles() > 0;
    }
    public enum MemberRemovalResult { UNKNOWN_MEMBER, MEMBERS_REMAIN, TRAIN_EMPTY }
    public synchronized void updateTrainPresence(UUID trainId, TrainPresence presence) {
        TrainRecord existing = trainRegistry.get(trainId);
        if (existing == null || presence == null || existing.presence() == presence) return;
        if (pendingTrainDeletions.contains(trainId) && presence != TrainPresence.DESTROYED) return;
        trainRegistry.put(trainId, new TrainRecord(existing.trainId(), existing.number(),
                existing.depotGenerated(), existing.lineId(), existing.controlMode(), presence));
        setDirty();
    }
    public synchronized String stationHistoryText(UUID trainId) {
        return stationHistory.getOrDefault(trainId, "");
    }

    public synchronized void recordStationArrival(UUID trainId, RailwayDevice station) {
        TrainRecord train = trainRegistry.get(trainId);
        if (station.type() != DeviceType.STATION || train == null || !train.present()
                || isTrainPendingDeletion(trainId)) return;
        String previous = stationHistoryText(trainId);
        String next = previous.isEmpty() ? station.name() : previous + " → " + station.name();
        if (next.length() > STATION_HISTORY_TEXT_LIMIT) {
            int cutoff = next.length() - STATION_HISTORY_TEXT_LIMIT + 2;
            int separator = next.indexOf(" → ", cutoff);
            if (separator >= 0) next = next.substring(separator + 3);
            else next = station.name();
            next = "… " + next;
        }
        stationHistory.put(trainId, next);
        setDirty();
    }

    public synchronized void retireTrain(UUID trainId) {
        boolean changed = activeTrains.remove(trainId) != null;
        changed |= trainManifests.remove(trainId) != null;
        changed |= stationHistory.remove(trainId) != null;
        TrainRecord existing = trainRegistry.get(trainId);
        if (existing != null && existing.present()) {
            trainRegistry.put(trainId, new TrainRecord(existing.trainId(), existing.number(),
                    existing.depotGenerated(), existing.lineId(), existing.controlMode(), TrainPresence.DESTROYED));
            changed = true;
        }
        if (changed) setDirty();
    }
    public synchronized void markTrainForDeletion(UUID trainId) {
        if (trainId == null) return;
        retireTrain(trainId);
        if (pendingTrainDeletions.add(trainId)) setDirty();
    }
    public synchronized boolean isTrainPendingDeletion(UUID trainId) {
        return trainId != null && pendingTrainDeletions.contains(trainId);
    }
    public synchronized void removeTrainRecord(UUID trainId) {
        retireTrain(trainId);
    }

    public synchronized void clearActiveTrainsForRestart() {
        if (!activeTrains.isEmpty()) { activeTrains.clear(); setDirty(); }
    }

    public synchronized void prepareTrainRegistryForRestart() {
        boolean changed = !activeTrains.isEmpty();
        activeTrains.clear();
        for (Map.Entry<UUID, TrainRecord> entry : trainRegistry.entrySet()) {
            TrainRecord record = entry.getValue();
            if (record.presence() != TrainPresence.LOADED) continue;
            entry.setValue(new TrainRecord(record.trainId(), record.number(), record.depotGenerated(),
                    record.lineId(), record.controlMode(), TrainPresence.LEGACY_UNCONFIRMED));
            changed = true;
        }
        if (changed) setDirty();
    }

    public synchronized double classSpeedLimit(TrainClass trainClass) {
        return switch (trainClass) {
            case CARGO -> freightSpeedLimitKmh;
            case PASSENGER -> passengerSpeedLimitKmh;
            case EXPRESS -> expressSpeedLimitKmh;
        };
    }

    public synchronized void updateClassSpeedLimits(double freight, double passenger, double express) {
        if (Double.isFinite(freight) && freight >= 0) freightSpeedLimitKmh = Math.min(freight, SpeedRules.MAX_CONFIGURED_LIMIT_KMH);
        if (Double.isFinite(passenger) && passenger >= 0) passengerSpeedLimitKmh = Math.min(passenger, SpeedRules.MAX_CONFIGURED_LIMIT_KMH);
        if (Double.isFinite(express) && express >= 0) expressSpeedLimitKmh = Math.min(express, SpeedRules.MAX_CONFIGURED_LIMIT_KMH);
        setDirty();
    }

    public synchronized RailwayLine createLine(long depotId) {
        RailwayDevice depot = devices.get(depotId);
        if (depot == null || depot.type() != DeviceType.DEPOT) {
            throw new IllegalArgumentException("Line must start at an existing depot");
        }
        RailwayLine existing = lines.values().stream().filter(line -> line.depotId() == depotId).findFirst().orElse(null);
        if (existing != null) return existing;
        long id = nextLineId++;
        RailwayLine line = new RailwayLine(id, "line-" + id, depotId, java.util.List.of(),
                30, 80.0D, 1, LineStatus.NEEDS_VALIDATION);
        lines.put(id, line);
        setDirty();
        return line;
    }

    public synchronized RailwayLine createEmptyLine(String requestedName) {
        String base = requestedName == null || requestedName.isBlank() ? "线路" + nextLineId : requestedName.strip();
        String safe = base.substring(0, Math.min(64, base.length()));
        if (lines.values().stream().anyMatch(line -> line.name().equalsIgnoreCase(safe))) {
            throw new IllegalArgumentException("Duplicate line name");
        }
        long id = nextLineId++;
        RailwayLine line = new RailwayLine(id, safe, 0L, java.util.List.of(), 30, 80.0D, 1,
                LineStatus.NEEDS_VALIDATION);
        lines.put(id, line);
        setDirty();
        return line;
    }

    /**
     * Binds one depot as the dispatcher for one line. The depot stored in the route node list is a
     * separate recovery destination and is intentionally not changed here.
     *
     * @return the previous dispatcher displaced from the target line, or {@code 0}
     */
    public synchronized long bindDispatchDepot(long depotId, long lineId) {
        RailwayDevice depot = devices.get(depotId);
        if (depot == null || depot.type() != DeviceType.DEPOT) {
            throw new IllegalArgumentException("Dispatch depot must be an existing depot");
        }
        RailwayLine target = lineId == 0L ? null : lines.get(lineId);
        if (lineId != 0L && target == null) throw new IllegalArgumentException("Unknown line");
        if (target != null) {
            for (long nodeId : target.nodeIds()) {
                RailwayDevice node = devices.get(nodeId);
                if (node != null && !node.dimension().equals(depot.dimension())) {
                    throw new IllegalArgumentException("A dispatch depot and its line cannot cross dimensions");
                }
            }
        }

        long displacedDepotId = target == null || target.depotId() == depotId ? 0L : target.depotId();
        boolean changed = false;
        for (Map.Entry<Long, RailwayLine> entry : new java.util.ArrayList<>(lines.entrySet())) {
            RailwayLine line = entry.getValue();
            if (line.depotId() == depotId && line.id() != lineId) {
                lines.put(line.id(), withDispatchDepot(line, 0L));
                changed = true;
            }
        }
        if (target != null) {
            target = lines.get(lineId);
            if (target.depotId() != depotId) {
                lines.put(lineId, withDispatchDepot(target, depotId));
                changed = true;
            }
        }
        if (changed) setDirty();
        return displacedDepotId;
    }

    public synchronized long dispatchLineIdForDepot(long depotId) {
        return lines.values().stream().filter(line -> line.depotId() == depotId)
                .mapToLong(RailwayLine::id).findFirst().orElse(0L);
    }

    public synchronized long recoveryDepotId(long lineId) {
        RailwayLine line = lines.get(lineId);
        if (line == null) return 0L;
        return line.nodeIds().stream().map(devices::get).filter(java.util.Objects::nonNull)
                .filter(device -> device.type() == DeviceType.DEPOT)
                .mapToLong(RailwayDevice::id).findFirst().orElse(0L);
    }

    private static RailwayLine withDispatchDepot(RailwayLine line, long depotId) {
        return new RailwayLine(line.id(), line.name(), depotId, line.nodeIds(), line.departureIntervalSeconds(),
                line.speedLimitKmh(), line.maximumTrains(), LineStatus.NEEDS_VALIDATION, line.switchDirections(),
                line.stationStopTracks());
    }

    public synchronized RailwayLine insertLineNode(long lineId, int index, long deviceId) {
        RailwayLine line = lines.get(lineId);
        RailwayDevice device = devices.get(deviceId);
        if (line == null || device == null) throw new IllegalArgumentException("Unknown line or device");
        RailwayDevice dimensionAnchor = !line.nodeIds().isEmpty()
                ? devices.get(line.nodeIds().getFirst()) : devices.get(line.depotId());
        if (dimensionAnchor != null && !dimensionAnchor.dimension().equals(device.dimension()))
            throw new IllegalArgumentException("A line cannot cross dimensions");
        if (device.type() == DeviceType.DEPOT && line.nodeIds().stream().map(devices::get)
                .filter(java.util.Objects::nonNull).anyMatch(existing -> existing.type() == DeviceType.DEPOT))
            throw new IllegalArgumentException("A line can contain only one recovery depot");
        java.util.ArrayList<Long> nodes = new java.util.ArrayList<>(line.nodeIds());
        int insertion = Math.clamp(index, 0, nodes.size());
        nodes.add(insertion, deviceId);
        java.util.HashMap<Integer,Integer> directions = shiftDirections(line.switchDirections(), insertion, 1);
        java.util.HashMap<Integer,StopTrack> stopTracks = shiftIndexed(line.stationStopTracks(), insertion, 1);
        if (device.type() == DeviceType.SWITCH) directions.put(insertion, 1);
        RailwayLine updated = new RailwayLine(line.id(), line.name(), line.depotId(), nodes, line.departureIntervalSeconds(),
                line.speedLimitKmh(), line.maximumTrains(), LineStatus.NEEDS_VALIDATION, directions, stopTracks);
        lines.put(lineId, updated); setDirty(); return updated;
    }

    public synchronized RailwayLine setSwitchDirection(long lineId, int nodeIndex, int direction) {
        RailwayLine line = lines.get(lineId);
        if (line == null || nodeIndex < 0 || nodeIndex >= line.nodeIds().size()) return line;
        RailwayDevice device = devices.get(line.nodeIds().get(nodeIndex));
        if (device == null || device.type() != DeviceType.SWITCH) return line;
        java.util.HashMap<Integer,Integer> directions = new java.util.HashMap<>(line.switchDirections());
        directions.put(nodeIndex, Math.clamp(direction, 0, 2));
        RailwayLine updated = new RailwayLine(line.id(), line.name(), line.depotId(), line.nodeIds(),
                line.departureIntervalSeconds(), line.speedLimitKmh(), line.maximumTrains(), LineStatus.NEEDS_VALIDATION,
                directions, line.stationStopTracks());
        lines.put(lineId, updated); setDirty(); return updated;
    }

    private static java.util.HashMap<Integer,Integer> shiftDirections(java.util.Map<Integer,Integer> source, int from, int amount) {
        java.util.HashMap<Integer,Integer> result = new java.util.HashMap<>();
        source.forEach((index, direction) -> result.put(index >= from ? index + amount : index, direction));
        return result;
    }

    private static <T> java.util.HashMap<Integer,T> shiftIndexed(java.util.Map<Integer,T> source, int from, int amount) {
        java.util.HashMap<Integer,T> result = new java.util.HashMap<>();
        source.forEach((index, value) -> result.put(index >= from ? index + amount : index, value));
        return result;
    }

    public synchronized RailwayLine setStationStopTrack(long lineId, int nodeIndex, StopTrack stopTrack) {
        RailwayLine line = lines.get(lineId);
        if (line == null || nodeIndex < 0 || nodeIndex >= line.nodeIds().size() || stopTrack == null) return line;
        RailwayDevice device = devices.get(line.nodeIds().get(nodeIndex));
        if (device == null || device.type() != DeviceType.STATION) return line;
        java.util.HashMap<Integer,StopTrack> tracks = new java.util.HashMap<>(line.stationStopTracks());
        tracks.put(nodeIndex, stopTrack);
        RailwayLine updated = new RailwayLine(line.id(), line.name(), line.depotId(), line.nodeIds(),
                line.departureIntervalSeconds(), line.speedLimitKmh(), line.maximumTrains(),
                LineStatus.NEEDS_VALIDATION, line.switchDirections(), tracks);
        lines.put(lineId, updated);
        setDirty();
        return updated;
    }

    public synchronized RailwayLine appendLineNode(long lineId, long deviceId) {
        RailwayLine line = lines.get(lineId);
        if (line == null) throw new IllegalArgumentException("Unknown line or device");
        return insertLineNode(lineId, line.nodeIds().size(), deviceId);
    }

    public synchronized RailwayLine modifyLineNode(long lineId, int index, int action) {
        RailwayLine line = lines.get(lineId);
        if (line == null || index < 0 || index >= line.nodeIds().size()) return line;
        java.util.ArrayList<Long> nodes = new java.util.ArrayList<>(line.nodeIds());
        if (action == 0 && index > 0) java.util.Collections.swap(nodes, index, index - 1);
        else if (action == 1 && index < nodes.size() - 1) java.util.Collections.swap(nodes, index, index + 1);
        else if (action == 2) nodes.remove(index);
        else return line;
        java.util.HashMap<Integer,Integer> directions = new java.util.HashMap<>();
        java.util.HashMap<Integer,StopTrack> stopTracks = new java.util.HashMap<>();
        line.switchDirections().forEach((oldIndex, direction) -> {
            int newIndex = oldIndex;
            if (action == 0 && index > 0) { if (oldIndex == index) newIndex = index - 1; else if (oldIndex == index - 1) newIndex = index; }
            else if (action == 1 && index < line.nodeIds().size() - 1) { if (oldIndex == index) newIndex = index + 1; else if (oldIndex == index + 1) newIndex = index; }
            else if (action == 2) { if (oldIndex == index) return; if (oldIndex > index) newIndex = oldIndex - 1; }
            directions.put(newIndex, direction);
        });
        line.stationStopTracks().forEach((oldIndex, stopTrack) -> {
            int newIndex = oldIndex;
            if (action == 0 && index > 0) { if (oldIndex == index) newIndex = index - 1; else if (oldIndex == index - 1) newIndex = index; }
            else if (action == 1 && index < line.nodeIds().size() - 1) { if (oldIndex == index) newIndex = index + 1; else if (oldIndex == index + 1) newIndex = index; }
            else if (action == 2) { if (oldIndex == index) return; if (oldIndex > index) newIndex = oldIndex - 1; }
            stopTracks.put(newIndex, stopTrack);
        });
        RailwayLine updated = new RailwayLine(line.id(), line.name(), line.depotId(), nodes,
                line.departureIntervalSeconds(), line.speedLimitKmh(), line.maximumTrains(), LineStatus.NEEDS_VALIDATION,
                directions, stopTracks);
        lines.put(lineId, updated);
        setDirty();
        return updated;
    }

    public synchronized boolean deleteLine(long lineId) {
        boolean removed = lines.remove(lineId) != null;
        if (removed) setDirty();
        return removed;
    }

    public synchronized RailwayLine setLineStatus(long lineId, LineStatus status) {
        RailwayLine line = lines.get(lineId);
        if (line == null) return null;
        RailwayLine updated = new RailwayLine(line.id(), line.name(), line.depotId(), line.nodeIds(),
                line.departureIntervalSeconds(), line.speedLimitKmh(), line.maximumTrains(), status,
                line.switchDirections(), line.stationStopTracks());
        lines.put(lineId, updated);
        setDirty();
        return updated;
    }

    public synchronized RailwayLine updateLineSettings(long lineId, String name, int intervalSeconds, double speedKmh,
                                                        int maximumTrains, LineStatus status) {
        RailwayLine line = lines.get(lineId);
        if (line == null) return null;
        int interval = Math.clamp(intervalSeconds, 0, 3600);
        double speed = Double.isFinite(speedKmh) ? Math.max(0.0D, speedKmh) : line.speedLimitKmh();
        int maximum = Math.clamp(maximumTrains, 1, 1000);
        String safeName = name == null || name.isBlank() ? "line-" + line.id()
                : name.strip().substring(0, Math.min(64, name.strip().length()));
        RailwayLine updated = new RailwayLine(line.id(), safeName, line.depotId(), line.nodeIds(),
                interval, speed, maximum, status, line.switchDirections(), line.stationStopTracks());
        lines.put(lineId, updated);
        setDirty();
        return updated;
    }

    private static String defaultName(DeviceType type, long id) {
        return switch (type) {
            case STATION -> "站点" + id;
            case DEPOT -> "车辆段" + id;
            case SWITCH -> "道岔" + id;
            case FREIGHT_YARD -> "货场" + id;
        };
    }

    private final java.util.Map<Long,Boolean> stationContainers=new java.util.HashMap<>();
    public Boolean stationContainer(long id){return stationContainers.get(id);}
    public void stationContainer(long id,boolean connected){if(!java.util.Objects.equals(stationContainers.put(id,connected),connected))setDirty();}

    private long nextDefaultNumber(DeviceType type) {
        return switch (type) {
            case STATION -> nextStationNumber++;
            case SWITCH -> nextSwitchNumber++;
            case DEPOT -> nextDepotNumber++;
            case FREIGHT_YARD -> nextFreightYardNumber++;
        };
    }

    private static RailwaySavedData fromDynamic(Dynamic<?> dynamic) {
        CompoundTag tag = (CompoundTag) dynamic.convert(NbtOps.INSTANCE).getValue();
        RailwaySavedData data = new RailwaySavedData();
        for(String entry:(tag.getStringOr("StationContainers", "")).split(",")){String[] bits=entry.split(":");if(bits.length==2)try{data.stationContainers.put(Long.parseLong(bits[0]),bits[1].equals("1"));}catch(NumberFormatException ignored){}}
        int schemaVersion = tag.getIntOr("SchemaVersion", 0);
        int trainPresenceFormat = tag.getIntOr("TrainPresenceFormat", 0);
        data.nextDeviceId = Math.max(1L, tag.getLongOr("NextDeviceId", 1L));
        data.nextLineId = Math.max(1L, tag.getLongOr("NextLineId", 1L));
        data.nextStationNumber = Math.max(1L, tag.getLongOr("NextStationNumber", 1L));
        data.nextFreightYardNumber = Math.max(1L, tag.getLongOr("NextFreightYardNumber", 1L));
        data.nextSwitchNumber = Math.max(1L, tag.getLongOr("NextSwitchNumber", 1L));
        data.nextDepotNumber = Math.max(1L, tag.getLongOr("NextDepotNumber", 1L));
        data.freightSpeedLimitKmh = validSpeed(tag.getDoubleOr("FreightSpeedLimitKmh", SpeedRules.FREIGHT_LIMIT_KMH), SpeedRules.FREIGHT_LIMIT_KMH);
        data.passengerSpeedLimitKmh = validSpeed(tag.getDoubleOr("PassengerSpeedLimitKmh", SpeedRules.PASSENGER_LIMIT_KMH), SpeedRules.PASSENGER_LIMIT_KMH);
        data.expressSpeedLimitKmh = validSpeed(tag.getDoubleOr("ExpressSpeedLimitKmh", SpeedRules.EXPRESS_LIMIT_KMH), SpeedRules.EXPRESS_LIMIT_KMH);
        ListTag list = tag.getListOrEmpty("Devices");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompoundOrEmpty(i);
            long id = entry.getLongOr("Id", 0L);
            try {
                DeviceType type = DeviceType.valueOf(entry.getStringOr("Type", "STATION"));
                RailwayDevice device = new RailwayDevice(id, type,
                        entry.getStringOr("Dimension", "minecraft:overworld"),
                        entry.getIntOr("X", 0), entry.getIntOr("Y", 0), entry.getIntOr("Z", 0),
                        migrateDefaultName(type, id, entry.getStringOr("Name", defaultName(type, id))),
                        Math.clamp(entry.getIntOr("DwellSeconds", 30), 0, 3600),
                        Math.clamp(entry.getIntOr("CargoDwellSeconds", 120), 0, 3600),
                        Math.max(0, entry.getIntOr("SwitchShapeOrdinal", 0)), entry.getIntOr("SwitchEntryOrdinal", -1), entry.getStringOr("Announcement", ""),
                        entry.getBooleanOr("ManualUnload", false));
                if (id > 0) data.devices.put(id, device);
            } catch (IllegalArgumentException ignored) {
                // Unknown future device types are skipped instead of corrupting the world load.
            }
        }
        if (!tag.contains("NextStationNumber")) data.nextStationNumber = 1L + data.devices.values().stream().filter(d -> d.type() == DeviceType.STATION).count();
        if (!tag.contains("NextSwitchNumber")) data.nextSwitchNumber = 1L + data.devices.values().stream().filter(d -> d.type() == DeviceType.SWITCH).count();
        if (!tag.contains("NextDepotNumber")) data.nextDepotNumber = 1L + data.devices.values().stream().filter(d -> d.type() == DeviceType.DEPOT).count();
        ListTag lineList = tag.getListOrEmpty("Lines");
        for (int i = 0; i < lineList.size(); i++) {
            CompoundTag entry = lineList.getCompoundOrEmpty(i);
            java.util.ArrayList<Long> nodes = new java.util.ArrayList<>();
            ListTag nodeList = entry.getListOrEmpty("Nodes");
            for (int j = 0; j < nodeList.size(); j++) {
                nodes.add(nodeList.getCompoundOrEmpty(j).getLongOr("Id", 0L));
            }
            java.util.HashMap<Integer,Integer> directions = new java.util.HashMap<>();
            ListTag directionList = entry.getListOrEmpty("SwitchDirections");
            for (int j = 0; j < directionList.size(); j++) {
                CompoundTag direction = directionList.getCompoundOrEmpty(j);
                directions.put(direction.getIntOr("Index", -1), direction.getIntOr("Direction", -1));
            }
            java.util.HashMap<Integer,StopTrack> stopTracks = new java.util.HashMap<>();
            ListTag stopTrackList = entry.getListOrEmpty("StationStopTracks");
            for (int j = 0; j < stopTrackList.size(); j++) {
                CompoundTag stop = stopTrackList.getCompoundOrEmpty(j);
                int index = stop.getIntOr("Index", -1);
                if (index >= 0) stopTracks.put(index, new StopTrack(stop.getIntOr("X", 0),
                        stop.getIntOr("Y", 0), stop.getIntOr("Z", 0)));
            }
            try {
                RailwayLine line = new RailwayLine(
                        entry.getLongOr("Id", 0L), entry.getStringOr("Name", "line"),
                        entry.getLongOr("DepotId", 0L), nodes,
                        entry.getIntOr("DepartureIntervalSeconds", 30),
                        entry.getDoubleOr("SpeedLimitKmh", 80.0D),
                        entry.getIntOr("MaximumTrains", 1),
                        LineStatus.valueOf(entry.getStringOr("Status", "NEEDS_VALIDATION")), directions, stopTracks);
                data.lines.put(line.id(), line);
            } catch (IllegalArgumentException ignored) {
                // Invalid lines are skipped so one bad record cannot prevent world loading.
            }
        }
        ListTag trainList = tag.getListOrEmpty("ActiveTrains");
        for (int i = 0; i < trainList.size(); i++) {
            CompoundTag entry = trainList.getCompoundOrEmpty(i);
            try {
                UUID trainId = UUID.fromString(entry.getStringOr("TrainId", ""));
                long lineId = entry.getLongOr("LineId", 0L);
                if (lineId > 0) data.activeTrains.put(trainId, lineId);
            } catch (IllegalArgumentException ignored) { }
        }
        data.nextTrainNumber = Math.max(1L, tag.getLongOr("NextTrainNumber", 1L));
        java.util.HashSet<Long> usedTrainNumbers = new java.util.HashSet<>();
        ListTag registryList = tag.getListOrEmpty("TrainRegistry");
        for (int i = 0; i < registryList.size(); i++) {
            CompoundTag entry = registryList.getCompoundOrEmpty(i);
            try {
                UUID trainId = UUID.fromString(entry.getStringOr("TrainId", ""));
                long number = Math.max(1L, entry.getLongOr("Number", 1L));
                if (!usedTrainNumbers.add(number)) {
                    while (usedTrainNumbers.contains(data.nextTrainNumber)) data.nextTrainNumber++;
                    number = data.nextTrainNumber++;
                    usedTrainNumbers.add(number);
                }
                boolean depotGenerated=entry.getBooleanOr("DepotGenerated", false);
                long lineId=entry.getLongOr("LineId", 0L);
                ControlMode controlMode;
                try { controlMode=ControlMode.valueOf(entry.getStringOr("ControlMode",
                        depotGenerated&&lineId>0?ControlMode.ROUTE_AUTOMATIC.name():ControlMode.MANUAL.name())); }
                catch(IllegalArgumentException ignored){controlMode=depotGenerated&&lineId>0?ControlMode.ROUTE_AUTOMATIC:ControlMode.MANUAL;}
                data.trainRegistry.put(trainId, new TrainRecord(trainId, number,
                        depotGenerated, lineId, controlMode,
                        TrainRecordPresence.readLifecycle(entry, trainPresenceFormat)));
                String history = entry.getStringOr("StationHistory", "");
                if (!history.isEmpty() && history.length() <= STATION_HISTORY_TEXT_LIMIT)
                    data.stationHistory.put(trainId, history);
                long originDepotNumber = Math.max(0L, entry.getLongOr("OriginDepotNumber", 0L));
                ListTag memberList = entry.getListOrEmpty("Members");
                java.util.ArrayList<TrainMember> members = new java.util.ArrayList<>();
                for (int memberIndex = 0; memberIndex < memberList.size(); memberIndex++) {
                    CompoundTag member = memberList.getCompoundOrEmpty(memberIndex);
                    try {
                        members.add(new TrainMember(
                                UUID.fromString(member.getStringOr("EntityId", "")),
                                TrainVehicleKind.valueOf(member.getStringOr("Kind", "PASSENGER")),
                                Math.max(1, member.getIntOr("Sequence", memberIndex + 1))));
                    } catch (IllegalArgumentException ignored) { }
                }
                if (!members.isEmpty()) data.trainManifests.put(trainId,
                        new TrainManifest(originDepotNumber, members));
                data.nextTrainNumber = Math.max(data.nextTrainNumber, number + 1L);
            } catch (IllegalArgumentException ignored) { }
        }
        ListTag deletionList = tag.getListOrEmpty("PendingTrainDeletions");
        for (int i = 0; i < deletionList.size(); i++) {
            try {
                data.pendingTrainDeletions.add(UUID.fromString(
                        deletionList.getCompoundOrEmpty(i).getStringOr("TrainId", "")));
            } catch (IllegalArgumentException ignored) { }
        }
        for (long id : data.devices.keySet()) data.nextDeviceId = Math.max(data.nextDeviceId, id + 1L);
        for (long id : data.lines.keySet()) data.nextLineId = Math.max(data.nextLineId, id + 1L);
        if (schemaVersion < CURRENT_SCHEMA_VERSION) data.setDirty();
        return data;
    }

    private Dynamic<?> toDynamic() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("SchemaVersion", CURRENT_SCHEMA_VERSION);
        tag.putLong("NextDeviceId", nextDeviceId);
        tag.putLong("NextLineId", nextLineId);
        tag.putLong("NextStationNumber", nextStationNumber);
        tag.putLong("NextFreightYardNumber", nextFreightYardNumber);
        tag.putLong("NextSwitchNumber", nextSwitchNumber);
        tag.putLong("NextDepotNumber", nextDepotNumber);
        tag.putLong("NextTrainNumber", nextTrainNumber);
        tag.putInt("TrainPresenceFormat", TrainRecordPresence.CURRENT_FORMAT);
        tag.putString("StationContainers",stationContainers.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).map(e->e.getKey()+":"+(e.getValue()?"1":"0")).collect(java.util.stream.Collectors.joining(",")));
        tag.putDouble("FreightSpeedLimitKmh", freightSpeedLimitKmh);
        tag.putDouble("PassengerSpeedLimitKmh", passengerSpeedLimitKmh);
        tag.putDouble("ExpressSpeedLimitKmh", expressSpeedLimitKmh);
        ListTag list = new ListTag();
        devices.values().forEach(device -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Id", device.id());
            entry.putString("Type", device.type().name());
            entry.putString("Dimension", device.dimension());
            entry.putInt("X", device.x());
            entry.putInt("Y", device.y());
            entry.putInt("Z", device.z());
            entry.putString("Name", device.name());
            entry.putInt("DwellSeconds", device.dwellSeconds());
            entry.putInt("CargoDwellSeconds", device.cargoDwellSeconds());
            entry.putInt("SwitchShapeOrdinal", device.switchShapeOrdinal());
            entry.putInt("SwitchEntryOrdinal", device.switchEntryOrdinal());
            entry.putString("Announcement", device.announcement());
            entry.putBoolean("ManualUnload", device.manualUnload());
            list.add(entry);
        });
        tag.put("Devices", list);
        ListTag lineList = new ListTag();
        lines.values().forEach(line -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Id", line.id());
            entry.putString("Name", line.name());
            entry.putLong("DepotId", line.depotId());
            entry.putInt("DepartureIntervalSeconds", line.departureIntervalSeconds());
            entry.putDouble("SpeedLimitKmh", line.speedLimitKmh());
            entry.putInt("MaximumTrains", line.maximumTrains());
            entry.putString("Status", line.status().name());
            ListTag nodes = new ListTag();
            line.nodeIds().forEach(nodeId -> {
                CompoundTag node = new CompoundTag();
                node.putLong("Id", nodeId);
                nodes.add(node);
            });
            entry.put("Nodes", nodes);
            ListTag directions = new ListTag();
            line.switchDirections().forEach((index, direction) -> {
                CompoundTag value = new CompoundTag();
                value.putInt("Index", index); value.putInt("Direction", direction); directions.add(value);
            });
            entry.put("SwitchDirections", directions);
            ListTag stopTracks = new ListTag();
            line.stationStopTracks().forEach((index, stopTrack) -> {
                CompoundTag value = new CompoundTag();
                value.putInt("Index", index);
                value.putInt("X", stopTrack.x());
                value.putInt("Y", stopTrack.y());
                value.putInt("Z", stopTrack.z());
                stopTracks.add(value);
            });
            entry.put("StationStopTracks", stopTracks);
            lineList.add(entry);
        });
        tag.put("Lines", lineList);
        ListTag trainList = new ListTag();
        activeTrains.forEach((trainId, lineId) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("TrainId", trainId.toString());
            entry.putLong("LineId", lineId);
            trainList.add(entry);
        });
        tag.put("ActiveTrains", trainList);
        ListTag registryList = new ListTag();
        trainRegistry.values().forEach(record -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("TrainId", record.trainId().toString());
            entry.putLong("Number", record.number());
            entry.putBoolean("DepotGenerated", record.depotGenerated());
            entry.putLong("LineId", record.lineId());
            entry.putString("ControlMode", record.controlMode().name());
            entry.putBoolean("Present", record.present());
            entry.putString("Presence", record.presence().name());
            TrainManifest manifest = trainManifests.get(record.trainId());
            if (manifest != null) {
                entry.putLong("OriginDepotNumber", manifest.originDepotNumber());
                ListTag members = new ListTag();
                manifest.members().forEach(member -> {
                    CompoundTag memberTag = new CompoundTag();
                    memberTag.putString("EntityId", member.entityId().toString());
                    memberTag.putString("Kind", member.kind().name());
                    memberTag.putInt("Sequence", member.sequence());
                    members.add(memberTag);
                });
                entry.put("Members", members);
            }
            String history = stationHistoryText(record.trainId());
            if (!history.isEmpty()) entry.putString("StationHistory", history);
            registryList.add(entry);
        });
        tag.put("TrainRegistry", registryList);
        ListTag deletionList = new ListTag();
        pendingTrainDeletions.forEach(trainId -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("TrainId", trainId.toString());
            deletionList.add(entry);
        });
        tag.put("PendingTrainDeletions", deletionList);
        return new Dynamic<>(NbtOps.INSTANCE, tag);
    }

    private static String migrateDefaultName(DeviceType type, long id, String name) {
        if (name.equals("station-" + id) || name.equals("switch-" + id) || name.equals("depot-" + id)) {
            return defaultName(type, id);
        }
        return name;
    }

    private static double validSpeed(double value, double fallback) { return Double.isFinite(value) && value >= 0 ? value : fallback; }
}
