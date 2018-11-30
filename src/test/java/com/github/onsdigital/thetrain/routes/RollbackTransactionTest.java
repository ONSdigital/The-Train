package com.github.onsdigital.thetrain.routes;

import spark.Route;

public class RollbackTransactionTest extends BaseRouteTest {

    private Route route;

    @Override
    public void customSetUp() throws Exception {
        route = new RollbackTransaction(transactionsService, publisherService);
    }
}
