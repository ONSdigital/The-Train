package com.github.onsdigital.thetrain.routes;

import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.service.TransactionsService;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static java.lang.String.format;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

public class GetTransaction extends BaseHandler {

    static final String GET_TRANS_SUCCESS_LOG = "get transaction completed successfully";
    static final String GET_TRANS_SUCCESS_RESULT = "Details for transaction %s";

    private TransactionsService transactionsService;

    public GetTransaction(TransactionsService transactionsService) {
        this.transactionsService = transactionsService;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        Transaction transaction = transactionsService.getTransaction(request);

        response.status(OK_200);
        info().data("transaction_id", transaction.id()).log(GET_TRANS_SUCCESS_LOG);
        return new Result(format(GET_TRANS_SUCCESS_RESULT, transaction.id()), false, transaction);
    }
}
