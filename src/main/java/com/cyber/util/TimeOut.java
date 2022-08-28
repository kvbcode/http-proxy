package com.cyber.util;

public class TimeOut {

    private volatile long timeoutNanos;
    private volatile long lastUpdateNanos;

    private TimeOut(long timeoutNanos) {
        this.timeoutNanos = timeoutNanos;
        this.lastUpdateNanos = System.nanoTime();
    }

    public static TimeOut fromSeconds(int seconds){
        return new TimeOut(seconds * 1_000_000_000L);
    }

    public static TimeOut fromMillis(int millis){
        return new TimeOut(millis * 1_000_000L);
    }

    public boolean isTimeout(){
        return (System.nanoTime() - lastUpdateNanos) > timeoutNanos;
    }

    public void update(){
        this.lastUpdateNanos = System.nanoTime();
    }

}
