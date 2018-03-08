package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.json.Result;
import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.davidcarboni.thetrain.storage.Publisher;
import com.github.davidcarboni.thetrain.storage.Transactions;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

import static com.github.davidcarboni.thetrain.logging.LogBuilder.error;
import static com.github.davidcarboni.thetrain.logging.LogBuilder.info;
import static com.github.davidcarboni.thetrain.logging.LogBuilder.warn;

/**
 * API to roll back an existing {@link Transaction}.
 */
@Api
public class Rollback {

    @POST
    public Result rollback(HttpServletRequest request,
                           HttpServletResponse response) throws IOException, FileUploadException {

        Transaction transaction = null;
        String transactionId = null;
        String message = null;
        boolean isError = false;

        try {
            // Transaction ID
            transactionId = request.getParameter("transactionId");
            if (StringUtils.isBlank(transactionId)) {
                warn("rollback: transactionID required but none provided").log();
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                isError = true;
                message = "Please provide a transactionId parameter.";
            }

            // Transaction object
            String encryptionPassword = request.getParameter("encryptionPassword");
            transaction = Transactions.get(transactionId, encryptionPassword);
            if (transaction == null) {
                warn("rollback: transaction not found")
                        .transactionID(transactionId)
                        .log();
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                isError = true;
                message = "Unknown transaction " + transactionId;
            }

            // Check the transaction state
            if (transaction != null && !transaction.isOpen()) {
                warn("rollback: unexpected error transaction closed")
                        .transactionID(transactionId)
                        .log();
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                isError = true;
                message = "This transaction is closed.";
            }

            // Roll back
            if (!isError) {
                info("rollback: no errors setting up rollback, proceeding")
                        .transactionID(transactionId)
                        .log();
                boolean result = Publisher.rollback(transaction);
                if (!result) {
                    warn("rollback: rollback failed")
                            .transactionID(transactionId)
                            .log();
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    isError = true;
                    message = "Errors were detected in rolling back the transaction.";
                } else {
                    info("rollback: rollback completed successfully")
                            .transactionID(transactionId)
                            .log();
                    message = "Transaction rolled back.";
                    Transactions.listFiles(transaction);
                }
            }

        } catch (Exception e) {
            error(e, "rollback: unexpected error while attempting to rollback transaction")
                    .transactionID(transactionId)
                    .log();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            isError = true;
            message = ExceptionUtils.getStackTrace(e);
        } finally {
            info("rollback: updating transaction")
                    .transactionID(transactionId)
                    .log();
            Transactions.update(transaction);
        }
        return new Result(message, isError, transaction);
    }
}
