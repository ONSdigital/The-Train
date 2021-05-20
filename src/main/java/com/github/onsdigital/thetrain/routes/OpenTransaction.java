package com.github.onsdigital.thetrain.routes;

import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.service.TransactionsService;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.logging.TrainEvent.error;
import static com.github.onsdigital.thetrain.logging.TrainEvent.info;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static java.lang.String.format;

public class OpenTransaction extends BaseHandler {

    static final String SUCCESS_MSG = "New transaction created.";
    static final String ERROR_MSG = "Error creating transaction.";
    static final String ERROR_AND_INSERT_MSG = ERROR_MSG + " %s";

    private TransactionsService transactionsService;

    public OpenTransaction(TransactionsService transactionsService) {
        this.transactionsService = transactionsService;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception  {
        Transaction transaction = null;
        try {
            transaction = transactionsService.create();
        } catch (PublishException e) {
            transaction.setStatus(Transaction.COMMIT_FAILED);
            transaction.addError(format(ERROR_AND_INSERT_MSG, e.getMessage()));
            error().log(format(ERROR_AND_INSERT_MSG, e.getMessage()));
            throw new PublishException(ERROR_MSG,e);
        }

        info().transactionID(transaction.id()).log("transaction created successfully");

        response.status(OK_200);
        if (transaction.getStatus().equals(null)) {
            transaction.setStatus(Transaction.COMMITTED);
        }
        return new Result(SUCCESS_MSG, false, transaction);
    }
}
