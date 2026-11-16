package cn.quit5700.fabrictrain.data;


import cn.quit5700.fabrictrain.compat.NbtCompat;
import net.minecraft.nbt.CompoundTag;

/**
 * Old saves have no presence marker. They remain as hidden identities so a train entity loaded
 * later can reclaim its stable number without putting historical records back in the visible list.
 */
public final class TrainRecordPresence {
    public static final int CURRENT_FORMAT = 3;

    private TrainRecordPresence() {}

    public static boolean read(CompoundTag entry) {
        return NbtCompat.getBoolean(entry, "Present", false);
    }

    public static boolean read(CompoundTag entry, int storedFormat) {
        return storedFormat >= CURRENT_FORMAT && read(entry);
    }

    public static TrainPresence readLifecycle(CompoundTag entry, int storedFormat) {
        if (storedFormat < CURRENT_FORMAT) return TrainPresence.LEGACY_UNCONFIRMED;
        String value = NbtCompat.getString(entry, "Presence", "");
        try { return TrainPresence.valueOf(value); }
        catch (IllegalArgumentException ignored) {
            return NbtCompat.getBoolean(entry, "Present", false) ? TrainPresence.LOADED : TrainPresence.DESTROYED;
        }
    }
}
