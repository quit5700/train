package cn.quit5700.fabrictrain.data;

import net.minecraft.nbt.CompoundTag;

/**
 * Old saves have no presence marker. They remain as hidden identities so a train entity loaded
 * later can reclaim its stable number without putting historical records back in the visible list.
 */
public final class TrainRecordPresence {
    public static final int CURRENT_FORMAT = 3;

    private TrainRecordPresence() {}

    public static boolean read(CompoundTag entry) {
        return entry.getBooleanOr("Present", false);
    }

    public static boolean read(CompoundTag entry, int storedFormat) {
        return storedFormat >= CURRENT_FORMAT && read(entry);
    }

    public static TrainPresence readLifecycle(CompoundTag entry, int storedFormat) {
        if (storedFormat < CURRENT_FORMAT) return TrainPresence.LEGACY_UNCONFIRMED;
        String value = entry.getStringOr("Presence", "");
        try { return TrainPresence.valueOf(value); }
        catch (IllegalArgumentException ignored) {
            return entry.getBooleanOr("Present", false) ? TrainPresence.LOADED : TrainPresence.DESTROYED;
        }
    }
}
