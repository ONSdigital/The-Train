package com.github.onsdigital.thetrain.handlers;

import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.logging.LogBuilder;
import com.github.onsdigital.thetrain.service.TransactionsService;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.logging.LogBuilder.logBuilder;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

public class OpenTransaction extends BaseHandler {

    static final String SUCCESS_MSG = "New transaction created.";

    private TransactionsService transactionsService;

    public OpenTransaction(TransactionsService transactionsService) {
        this.transactionsService = transactionsService;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        LogBuilder log = logBuilder();

        log.info("creating new publishing transaction");
        Transaction transaction = transactionsService.create();
        log.transactionID(transaction.id()).info("transaction created successfully");

        response.status(OK_200);
        return new Result(SUCCESS_MSG, false, transaction);
    }
}
