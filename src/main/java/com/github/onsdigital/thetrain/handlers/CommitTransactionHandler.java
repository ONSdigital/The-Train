package com.github.onsdigital.thetrain.handlers;

import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.logging.LogBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import spark.Request;
import spark.Response;

import java.nio.file.Path;

import static com.github.onsdigital.thetrain.logging.LogBuilder.logBuilder;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Endpoint to commit an existing {@link Transaction}.
 */
public class CommitTransactionHandler extends BaseHandler {

    @Override
    public Object handle(Request request, Response response) throws Exception {
        Transaction transaction = null;
        String transactionId = null;

        LogBuilder log = logBuilder();

        try {
            // Transaction ID
            transactionId = request.params(TRANSACTION_ID_KEY);
            if (StringUtils.isBlank(transactionId)) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: transactionID required but none provided");
                response.status(BAD_REQUEST_400);
                return new Result("Please provide a transactionId parameter.", true, transaction);
            }

            log.transactionID(transactionId);

            // Transaction object
            transaction = getTransactionsService().get(transactionId);
            if (transaction == null) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: transaction was not found");
                response.status(BAD_REQUEST_400);
                return new Result("Unknown transaction " + transactionId, true, null);
            }

            // Check the transaction state
            if (!transaction.isOpen()) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("unexpected error, transaction is closed");
                response.status(BAD_REQUEST_400);
                return new Result("This transaction is closed.", true, transaction);
            }

            // Check for errors in the transaction
            if (transaction.hasErrors()) {
                log.responseStatus(BAD_REQUEST_400)
                        .errors(transaction.errors()).warn("bad request: transaction has errors");
                response.status(BAD_REQUEST_400);
                return new Result("This transaction cannot be committed because errors have been reported.", true, transaction);
            }

            // Get the website Path to publish to
            Path website = getPublisherService().websitePath();
            if (website == null) {
                log.responseStatus(INTERNAL_SERVER_ERROR_500)
                        .warn("transaction commit error - website path is null");
                response.status(INTERNAL_SERVER_ERROR_500);
                return new Result("website folder could not be used: " + website, true, transaction);
            }

            log.websitePath(website).info("request valid proceeding with committing transaction");

            boolean commitSuccessful = getPublisherService().commit(transaction, website);
            if (!commitSuccessful) {
                log.responseStatus(INTERNAL_SERVER_ERROR_500)
                        .warn("commiting publish to website was unsuccessful");
                response.status(INTERNAL_SERVER_ERROR_500);
                return new Result("Errors were detected in committing the transaction.", true, transaction);
            }

            // no errors return success response
            log.responseStatus(OK_200)
                    .info("commiting publish to website completed successfully");
            response.status(OK_200);
            return new Result("Transaction committed.", false, transaction);

        } catch (Exception e) {
            log.responseStatus(INTERNAL_SERVER_ERROR_500)
                    .error(e, "transaction returned unexpected error");
            response.status(INTERNAL_SERVER_ERROR_500);
            return new Result(ExceptionUtils.getStackTrace(e), true, transaction);
        } finally {
            log.info("persisting changes to transaction");
            getTransactionsService().update(transaction);
        }
    }
}
