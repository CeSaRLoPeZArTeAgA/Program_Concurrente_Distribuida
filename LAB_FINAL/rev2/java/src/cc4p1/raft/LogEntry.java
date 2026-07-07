package cc4p1.raft;

public final class LogEntry {
    public final long index;
    public final long term;
    public final String payload;

    public LogEntry(long index, long term, String payload) {
        this.index = index;
        this.term = term;
        this.payload = payload;
    }
}
