package cxsar.utils;

public class Timer {

    private long time;

    public Timer() {
        this.time = System.currentTimeMillis();
    }

    public void begin() {
        this.time = System.currentTimeMillis();
    }

    public long end() {
        return System.currentTimeMillis() - this.time;
    }
}
