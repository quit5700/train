package cn.quit5700.fabrictrain.depot;

public final class DepotDirectionRules {
    private DepotDirectionRules() {}

    public static int frontIndex(boolean firstHasNode, boolean secondHasNode) {
        if (firstHasNode == secondHasNode) return -1;
        return firstHasNode ? 0 : 1;
    }

    public static int frontIndex(int firstDistance, int secondDistance) {
        if (firstDistance < 0 && secondDistance < 0) return -1;
        if (firstDistance < 0) return 1;
        if (secondDistance < 0) return 0;
        if (firstDistance == secondDistance) return -1;
        return firstDistance < secondDistance ? 0 : 1;
    }
}
