package com.github.onsdigital.thetrain.handlers;

import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.logging.LogBuilder;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static com.github.onsdigital.thetrain.logging.LogBuilder.logBuilder;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

public class OpenTransaction extends BaseHandler {

    @Override
    public Object handle(Request request, Response response) throws Exception {
        LogBuilder log = logBuilder();
        Transaction transaction = null;

        try {
            log.info("creating new publishing transaction");
            transaction = getTransactionsService().create();
            log.transactionID(transaction.id()).info("transaction created successfully");

            response.status(OK_200);
            return new Result("New transaction created.", false, transaction);

        } catch (IOException e) {
            response.status(INTERNAL_SERVER_ERROR_500);
            log.error(e, "unexpected error while attempting to create transaction");
            return new Result("beingTransaction encountered unexpected error", true, transaction);
        }
    }
}
