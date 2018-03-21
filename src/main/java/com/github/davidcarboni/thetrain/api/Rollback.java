package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.api.common.Endpoint;
import com.github.davidcarboni.thetrain.json.Result;
import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.davidcarboni.thetrain.logging.Logger;
import com.github.davidcarboni.thetrain.storage.Publisher;
import com.github.davidcarboni.thetrain.storage.Transactions;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

import static com.github.davidcarboni.thetrain.logging.Logger.newLogger;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;

/**
 * Endpoint to roll back an existing {@link Transaction}.
 */
@Api
public class Rollback implements Endpoint {

    @POST
    public Result rollback(HttpServletRequest request,
                           HttpServletResponse response) throws IOException, FileUploadException {

        Logger logger = newLogger().endpoint(this);
        Transaction transaction = null;
        String transactionId = null;
        String message = null;
        boolean isError = false;

        try {
            // Transaction ID
            transactionId = request.getParameter(TRANSACTION_ID_KEY);
            if (StringUtils.isBlank(transactionId)) {
                logger.responseStatus(BAD_REQUEST_400)
                        .warn("transactionID required but none provided");
                response.setStatus(BAD_REQUEST_400);
                return new Result("Please provide a transactionId parameter.", true, null);
            }

            logger.transactionID(transactionId);

            // Transaction object
            String encryptionPassword = request.getParameter(ENCRYPTION_PASSWORD_KEY);
            if (StringUtils.isEmpty(encryptionPassword)) {
                logger.responseStatus(BAD_REQUEST_400)
                        .warn("rollback requires encryptionPassword but none was provided");

                response.setStatus(BAD_REQUEST_400);
                return new Result("rollback requires encryptionPassword but none was provided", true, null);
            }

            transaction = Transactions.get(transactionId, encryptionPassword);
            if (transaction == null) {
                logger.responseStatus(BAD_REQUEST_400)
                        .warn("transaction with specified ID was not found");

                response.setStatus(BAD_REQUEST_400);
                return new Result("Unknown transaction " + transactionId, true, null);
            }

            // Check the transaction state
            if (transaction != null && !transaction.isOpen()) {
                logger.responseStatus(BAD_REQUEST_400)
                        .warn("unexpected error transaction is closed");

                response.setStatus(BAD_REQUEST_400);
                return new Result("This transaction is closed.", true, transaction);
            }

            logger.info("request is valid, proceeding with rollback");

            boolean success = Publisher.rollback(transaction);
            if (!success) {
                logger.responseStatus(INTERNAL_SERVER_ERROR_500)
                        .warn("rollback was unsuccessful");

                response.setStatus(INTERNAL_SERVER_ERROR_500);
                return new Result("Errors were detected in rolling back the transaction.", true, transaction);
            }

            logger.info("rollback completed successfully");
            Transactions.listFiles(transaction);
            return new Result("Transaction rolled back.", false, transaction);

        } catch (Exception e) {
            logger.responseStatus(INTERNAL_SERVER_ERROR_500)
                    .error(e, "unexpected error while rollbacking back transaction");

            response.setStatus(INTERNAL_SERVER_ERROR_500);
            return new Result(ExceptionUtils.getStackTrace(e), true, transaction);
        } finally {
            logger.info("updating transaction");
            try {
                Transactions.update(transaction);
            } catch (Exception e) {
                logger.responseStatus(INTERNAL_SERVER_ERROR_500)
                        .error(e, "unexpected error while updating transaction");

                return new Result("unexpected error while updating transaction", true, transaction);
            }
        }
    }
}
