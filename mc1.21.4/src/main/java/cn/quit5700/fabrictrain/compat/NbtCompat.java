package cn.quit5700.fabrictrain.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Preserves the post-1.21.5 optional NBT read semantics on older game versions. */
public final class NbtCompat {
    private NbtCompat() {}

    public static int getInt(CompoundTag tag, String key, int fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback;
    }
    public static long getLong(CompoundTag tag, String key, long fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getLong(key) : fallback;
    }
    public static double getDouble(CompoundTag tag, String key, double fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getDouble(key) : fallback;
    }
    public static boolean getBoolean(CompoundTag tag, String key, boolean fallback) {
        return tag.contains(key, Tag.TAG_BYTE) ? tag.getBoolean(key) : fallback;
    }
    public static String getString(CompoundTag tag, String key, String fallback) {
        return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : fallback;
    }
    public static ListTag getList(CompoundTag tag, String key) {
        return tag.getList(key, Tag.TAG_COMPOUND);
    }
    public static CompoundTag getCompound(ListTag list, int index) {
        return list.getCompound(index);
    }
}
