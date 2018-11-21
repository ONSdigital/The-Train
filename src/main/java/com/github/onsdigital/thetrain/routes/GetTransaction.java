package com.github.onsdigital.thetrain.routes;

import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.logging.LogBuilder;
import com.github.onsdigital.thetrain.service.TransactionsService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.logging.LogBuilder.logBuilder;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

public class GetTransaction extends BaseHandler {

    private TransactionsService transactionsService;

    public GetTransaction(TransactionsService transactionsService) {
        this.transactionsService = transactionsService;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        LogBuilder log = logBuilder();

        try {
            Transaction transaction = transactionsService.getTransaction(request);
            log.responseStatus(OK_200)
                    .transactionID(transaction)
                    .info("get transaction completed successfully");

            response.status(OK_200);
            return new Result("Details for transaction " + transaction.id(), false, transaction);

        } catch (Exception e) {
            log.responseStatus(INTERNAL_SERVER_ERROR_500)
                    .error(e, "unexpected error while attempting to get transaction");

            response.status(INTERNAL_SERVER_ERROR_500);
            return new Result(ExceptionUtils.getStackTrace(e), true, null);
        }
    }
}
