package com.github.davidcarboni.thetrain;

import com.github.davidcarboni.restolino.framework.Startup;

import static com.github.davidcarboni.thetrain.logging.LogBuilder.info;

public class StartUpNotification implements Startup {

    @Override
    public void init() {
        info("application start up completed").log();
    }
}
