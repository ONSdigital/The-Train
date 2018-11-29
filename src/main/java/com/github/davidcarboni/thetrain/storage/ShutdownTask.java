package com.github.davidcarboni.thetrain.storage;

import java.util.concurrent.ExecutorService;

import static com.github.davidcarboni.thetrain.logging.LogBuilder.logBuilder;

public class ShutdownTask extends Thread {

    private final ExecutorService pool;

    public ShutdownTask(ExecutorService pool) {
        this.pool = pool;
    }

    @Override
    public void run() {
        super.run();
        logBuilder().info("shutting down Publisher Thread Pool");
        pool.shutdown();
    }
}
