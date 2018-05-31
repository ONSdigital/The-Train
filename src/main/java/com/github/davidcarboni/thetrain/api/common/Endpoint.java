package com.github.davidcarboni.thetrain.api.common;

import com.github.davidcarboni.thetrain.service.PublisherService;
import com.github.davidcarboni.thetrain.service.PublisherServiceImpl;
import com.github.davidcarboni.thetrain.service.TransactionsService;
import com.github.davidcarboni.thetrain.service.TransactionsServiceImpl;

public abstract class Endpoint {

    public static final String TRANSACTION_ID_KEY = "transactionId";

    public static final String ZIP_KEY = "zip";

    public static final String URI_KEY = "uri";

    public static final String SHA1_KEY = "sha1";

    protected TransactionsService transactionsService;

    protected PublisherService publisherService;

    protected TransactionsService getTransactionsService() {
        if (transactionsService == null) {
            transactionsService = new TransactionsServiceImpl();
        }
        return transactionsService;
    }

    protected PublisherService getPublisherService() {
        if (publisherService == null) {
            publisherService = new PublisherServiceImpl();
        }
        return publisherService;
    }
}
