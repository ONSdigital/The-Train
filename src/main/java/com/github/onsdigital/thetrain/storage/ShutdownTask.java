package com.github.onsdigital.thetrain.storage;

import java.util.concurrent.ExecutorService;

import static com.github.onsdigital.thetrain.logging.TrainEvent.info;

public class ShutdownTask extends Thread {

    private final ExecutorService pool;

    public ShutdownTask(ExecutorService pool) {
        this.pool = pool;
    }

    @Override
    public void run() {
        super.run();
        info().log("shutting down Publisher Thread Pool");
        pool.shutdown();
    }
}
