package cn.quit5700.fabrictrain.route;

public final class StationIdSequence {
    private long nextId;

    public StationIdSequence(long lastIssuedId) {
        if (lastIssuedId < 0) {
            throw new IllegalArgumentException("Last issued station id cannot be negative");
        }
        nextId = Math.addExact(lastIssuedId, 1L);
    }

    public long issue() {
        long issued = nextId;
        nextId = Math.addExact(nextId, 1L);
        return issued;
    }

    public long nextId() {
        return nextId;
    }
}
