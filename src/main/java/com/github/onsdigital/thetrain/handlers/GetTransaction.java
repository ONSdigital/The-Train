package com.github.onsdigital.thetrain.handlers;

import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.logging.LogBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.logging.LogBuilder.logBuilder;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

public class GetTransaction extends BaseHandler {

    @Override
    public Object handle(Request request, Response response) throws Exception {
        LogBuilder log = logBuilder();
        Transaction transaction = null;
        String transactionID = null;

        try {
            // Transaction ID
            transactionID = request.raw().getParameter(TRANSACTION_ID_KEY);
            if (StringUtils.isBlank(transactionID)) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: transactionID required but none provided");

                response.status(BAD_REQUEST_400);
                return new Result("Please provide a transactionId parameter.", true, null);
            }

            log.transactionID(transactionID);

            transaction = getTransactionsService().get(transactionID);
            if (transaction == null) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: transaction with specified ID not found");

                response.status(BAD_REQUEST_400);
                return new Result("Unknown transaction " + transactionID, true, null);
            }

            log.responseStatus(OK_200)
                    .info("get transaction completed successfully");

            response.status(OK_200);
            return new Result("Details for transaction " + transaction.id(), false, transaction);

        } catch (Exception e) {
            log.responseStatus(INTERNAL_SERVER_ERROR_500)
                    .error(e, "unexpected error while attempting to get transaction");

            response.status(INTERNAL_SERVER_ERROR_500);
            return new Result(ExceptionUtils.getStackTrace(e), true, transaction);
        }
    }
}
