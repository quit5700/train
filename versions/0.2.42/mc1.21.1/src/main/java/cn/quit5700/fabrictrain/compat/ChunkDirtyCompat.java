package cn.quit5700.fabrictrain.compat;
import net.minecraft.world.level.chunk.ChunkAccess;
/** 仅修改已持有区块的保存标记，不通过世界重新查询区块。 */
public final class ChunkDirtyCompat {
 private ChunkDirtyCompat() {}
 public static void mark(ChunkAccess chunk) { chunk.setUnsaved(true); }
}
