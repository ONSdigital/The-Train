package com.github.onsdigital.thetrain.api;

import com.github.onsdigital.thetrain.api.common.Endpoint;
import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.logging.LogBuilder;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Endpoint to roll back an existing {@link com.github.onsdigital.thetrain.json.Transaction}.
 */
//@Api
public class Rollback extends Endpoint {

    //@POST
    public Result rollback(HttpServletRequest request,
                           HttpServletResponse response) throws IOException, FileUploadException {

        LogBuilder log = LogBuilder.logBuilder().endpoint(this);
        Transaction transaction = null;
        String transactionId = null;

        try {
            // Transaction ID
            transactionId = request.getParameter(TRANSACTION_ID_KEY);
            if (StringUtils.isBlank(transactionId)) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: transactionID required but none provided");
                response.setStatus(BAD_REQUEST_400);
                return new Result("Please provide a transactionId parameter.", true, null);
            }

            log.transactionID(transactionId);

            // Transaction object
            transaction = getTransactionsService().get(transactionId);
            if (transaction == null) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: transaction with specified ID was not found");

                response.setStatus(BAD_REQUEST_400);
                return new Result("Unknown transaction " + transactionId, true, null);
            }

            // Check the transaction state
            if (!transaction.isOpen()) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: unexpected error transaction is closed");

                response.setStatus(BAD_REQUEST_400);
                return new Result("This transaction is closed.", true, transaction);
            }

            log.info("request is valid, proceeding with rollback");

            boolean success = getPublisherService().rollback(transaction);
            if (!success) {
                log.responseStatus(INTERNAL_SERVER_ERROR_500)
                        .warn("rollback was unsuccessful");

                response.setStatus(INTERNAL_SERVER_ERROR_500);
                return new Result("Errors were detected in rolling back the transaction.", true, transaction);
            }

            getTransactionsService().listFiles(transaction);

            log.responseStatus(OK_200).info("rollback completed successfully");
            response.setStatus(OK_200);
            return new Result("Transaction rolled back.", false, transaction);

        } catch (Exception e) {
            log.responseStatus(INTERNAL_SERVER_ERROR_500)
                    .error(e, "unexpected error while rollbacking back transaction");

            response.setStatus(INTERNAL_SERVER_ERROR_500);
            return new Result(ExceptionUtils.getStackTrace(e), true, transaction);
        } finally {
            log.info("updating transaction");
            try {
                getTransactionsService().update(transaction);
            } catch (Exception e) {
                log.responseStatus(INTERNAL_SERVER_ERROR_500)
                        .error(e, "unexpected error while updating transaction");

                return new Result("unexpected error while updating transaction", true, transaction);
            }
        }
    }
}
