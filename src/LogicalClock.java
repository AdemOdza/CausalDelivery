public class LogicalClock {
    int time;
    int d;

    public LogicalClock() {
        this(1);
    }

    public LogicalClock(int d) {
        this(0, d);
    }

    public LogicalClock(int time, int d) {
        this.time = time;
        this.d = d;
    }

    public final int getTime() {
        return time;
    }

    public final void setTime(int time) {
        this.time = time;
    }

    public final void increment() {
        this.time += d;
    }
}
