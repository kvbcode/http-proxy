package com.cyber;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

public class StreamTransfer implements Runnable {

    private final Supplier<Boolean> workWhile;
    private final InputStream in;
    private final OutputStream out;

    public StreamTransfer(Supplier<Boolean> workWhile, InputStream in, OutputStream out) {
        this.workWhile = workWhile;
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        byte[] buf = new byte[8 * 1024];
        try {
            int available = 0;
            int cnt = 0;
            while (workWhile.get()) {
                available = in.available();

                if (available==0){
                    LockSupport.parkNanos(1000);
                    continue;
                }

                if (available > buf.length) available = buf.length;
                cnt = in.read(buf, 0, available);
                out.write(buf, 0, cnt);
            }
        } catch (IOException ex) {
            System.err.println("StreamTransfer error: " + ex.getMessage());
        }
    }

}
