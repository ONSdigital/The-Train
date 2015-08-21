package com.github.davidcarboni.thetrain.destination.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.destination.json.Result;
import com.github.davidcarboni.thetrain.destination.json.Transaction;
import com.github.davidcarboni.thetrain.destination.storage.Publisher;
import com.github.davidcarboni.thetrain.destination.storage.Transactions;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.PUT;
import java.io.IOException;

/**
 * API to roll back an existing {@link Transaction}.
 */
@Api
public class Rollback {

    @PUT
    public Result rollback(HttpServletRequest request,
                           HttpServletResponse response) throws IOException, FileUploadException {

        Transaction transaction = null;
        String message = null;
        boolean error = false;

        try {

            // Transaction ID
            String transactionId = request.getParameter("transactionId");
            if (StringUtils.isBlank(transactionId)) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                error = true;
                message = "Please provide a transactionId parameter.";
            }

            // Transaction object
            String encryptionPassword = request.getParameter("encryptionPassword");
            transaction = Transactions.get(transactionId, encryptionPassword);
            if (transaction == null) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                error = true;
                message = "Unknown transaction " + transactionId;
            }

            // Commit
            if (!error) {
                boolean result = Publisher.rollback(transaction);
                if (!result) {
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    error = true;
                    message = "Errors were detected in rolling back the transaction.";
                }
            }

        } catch (Throwable t) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            error = true;
            message = ExceptionUtils.getStackTrace(t);
        }

        return new Result(message, error, transaction);
    }
}
