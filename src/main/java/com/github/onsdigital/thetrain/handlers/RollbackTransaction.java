package com.github.onsdigital.thetrain.handlers;

import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.logging.LogBuilder;
import com.github.onsdigital.thetrain.service.PublisherService;
import com.github.onsdigital.thetrain.service.TransactionsService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.logging.LogBuilder.logBuilder;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

public class RollbackTransaction extends BaseHandler {

    private TransactionsService transactionsService;
    private PublisherService publisherService;

    public RollbackTransaction(TransactionsService transactionsService, PublisherService publisherService) {
        this.transactionsService = transactionsService;
        this.publisherService = publisherService;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        LogBuilder log = logBuilder();
        Transaction transaction = null;
        String transactionId = null;

        try {
            // Transaction ID
            transactionId = request.raw().getParameter(TRANSACTION_ID_KEY);
            if (StringUtils.isBlank(transactionId)) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: transactionID required but none provided");
                response.status(BAD_REQUEST_400);
                return new Result("Please provide a transactionId parameter.", true, null);
            }

            log.transactionID(transactionId);

            // Transaction object
            transaction = transactionsService.getTransaction(request);
            if (transaction == null) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: transaction with specified ID was not found");

                response.status(BAD_REQUEST_400);
                return new Result("Unknown transaction " + transactionId, true, null);
            }

            // Check the transaction state
            if (!transaction.isOpen()) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: unexpected error transaction is closed");

                response.status(BAD_REQUEST_400);
                return new Result("This transaction is closed.", true, transaction);
            }

            log.info("request is valid, proceeding with rollback");

            boolean success = publisherService.rollback(transaction);
            if (!success) {
                log.responseStatus(INTERNAL_SERVER_ERROR_500)
                        .warn("rollback was unsuccessful");

                response.status(INTERNAL_SERVER_ERROR_500);
                return new Result("Errors were detected in rolling back the transaction.", true, transaction);
            }

            transactionsService.listFiles(transaction);

            log.responseStatus(OK_200).info("rollback completed successfully");
            response.status(OK_200);
            return new Result("Transaction rolled back.", false, transaction);

        } catch (Exception e) {
            log.responseStatus(INTERNAL_SERVER_ERROR_500)
                    .error(e, "unexpected error while rollbacking back transaction");

            response.status(INTERNAL_SERVER_ERROR_500);
            return new Result(ExceptionUtils.getStackTrace(e), true, transaction);
        } finally {
            log.info("updating transaction");
            try {
                transactionsService.update(transaction);
            } catch (Exception e) {
                log.responseStatus(INTERNAL_SERVER_ERROR_500)
                        .error(e, "unexpected error while updating transaction");

                return new Result("unexpected error while updating transaction", true, transaction);
            }
        }
    }
}
