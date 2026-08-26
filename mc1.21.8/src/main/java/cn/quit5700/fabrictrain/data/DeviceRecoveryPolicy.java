package cn.quit5700.fabrictrain.data;

/** Decides when an old world needs block-based device recovery. */
public final class DeviceRecoveryPolicy {
    private DeviceRecoveryPolicy() { }

    public static boolean shouldScanLegacyBlocks(int deviceCount, int lineCount) {
        return deviceCount == 0 && lineCount == 0;
    }
}
