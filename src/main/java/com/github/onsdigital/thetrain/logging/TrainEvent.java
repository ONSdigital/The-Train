package com.github.onsdigital.thetrain.logging;

import com.github.onsdigital.logging.v2.event.BaseEvent;
import com.github.onsdigital.logging.v2.event.Severity;
import com.github.onsdigital.logging.v2.event.SimpleEvent;

import static com.github.onsdigital.logging.v2.DPLogger.logConfig;

public class TrainEvent extends BaseEvent<TrainEvent> {

    public static TrainEvent info() {
        return new TrainEvent(logConfig().getNamespace(), Severity.INFO);
    }

    public static TrainEvent error() {
        return new TrainEvent(logConfig().getNamespace(), Severity.ERROR);
    }

    public static TrainEvent fatal(Throwable t) {
        return new TrainEvent(logConfig().getNamespace(), Severity.FATAL).exception(t);
    }

    public TrainEvent(String namespace, Severity severity) {
        super(namespace, severity, logConfig().getLogStore());
    }

    public TrainEvent transactionID(String id) {
        data("transaction_id", id);
        return this;
    }

}
