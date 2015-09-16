package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.helpers.DateConverter;
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
import java.util.Date;

/**
 * API to roll back an existing {@link Transaction}.
 */
@Api
public class Rollback {

    @POST
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

            // Check the transaction state
            if (transaction != null && !transaction.isOpen()) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                error = true;
                message = "This transaction is closed.";
            }

            // Roll back
            if (!error) {
                boolean result = Publisher.rollback(transaction);
                if (!result) {
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    error = true;
                    message = "Errors were detected in rolling back the transaction.";
                } else {
                    message = "Transaction rolled back.";
                    Transactions.listFiles(transaction);
                }
            }

        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            error = true;
            message = ExceptionUtils.getStackTrace(e);
        }

        System.out.println(DateConverter.toString(new Date()) + " " + message + (transaction != null ? " (" + transaction.id() + ")" : ""));
        return new Result(message, error, transaction);
    }
}
