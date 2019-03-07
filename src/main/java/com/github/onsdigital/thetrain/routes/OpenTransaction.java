package com.github.onsdigital.thetrain.routes;

import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.service.TransactionsService;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

public class OpenTransaction extends BaseHandler {

    static final String SUCCESS_MSG = "New transaction created.";

    private TransactionsService transactionsService;

    public OpenTransaction(TransactionsService transactionsService) {
        this.transactionsService = transactionsService;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        Transaction transaction = transactionsService.create();

        info().data("transaction_id", transaction.id()).log("transaction created successfully");

        response.status(OK_200);
        return new Result(SUCCESS_MSG, false, transaction);
    }
}
