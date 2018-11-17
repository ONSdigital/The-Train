package com.github.onsdigital.thetrain.storage;

import com.github.onsdigital.thetrain.logging.LogBuilder;

import java.util.concurrent.ExecutorService;

public class ShutdownTask extends Thread {

    private final ExecutorService pool;

    public ShutdownTask(ExecutorService pool) {
        this.pool = pool;
    }

    @Override
    public void run() {
        super.run();
        LogBuilder.logBuilder().info("shutting down Publisher Thread Pool");
        pool.shutdown();
    }
}
