package cn.quit5700.fabrictrain.train;

import java.util.UUID;

public final class TrainSession {
    private static UUID current = new UUID(0L, 0L);
    private TrainSession() {}

    public static void beginServerSession() { current = UUID.randomUUID(); }
    public static UUID current() { return current; }
}
