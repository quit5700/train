package cn.quit5700.fabrictrain.train;

public final class ConnectorActionRules {
    private ConnectorActionRules() {}

    public enum Action {
        CANCEL_SELECTION,
        CONNECT,
        DISCONNECT_ADJACENT,
        REJECT_NON_ADJACENT
    }

    public static Action decide(boolean sameEntity, boolean sameFormation, int firstIndex, int secondIndex) {
        if (sameEntity) return Action.CANCEL_SELECTION;
        if (!sameFormation) return Action.CONNECT;
        return Math.abs(firstIndex - secondIndex) == 1
                ? Action.DISCONNECT_ADJACENT
                : Action.REJECT_NON_ADJACENT;
    }
}
