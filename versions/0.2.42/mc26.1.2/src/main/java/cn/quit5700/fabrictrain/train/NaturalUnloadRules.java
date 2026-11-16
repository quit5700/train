package cn.quit5700.fabrictrain.train;

/** Entity tracking can end before vanilla assigns an unload removal reason. */
public final class NaturalUnloadRules {
    private NaturalUnloadRules() {}
    public static boolean shouldHandoff(boolean chunkUnload, boolean noRemovalReason,
                                        boolean automatic, boolean locomotive) {
        return chunkUnload || (noRemovalReason && automatic && locomotive);
    }
}
