package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.json.Result;
import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.davidcarboni.thetrain.storage.Publisher;
import com.github.davidcarboni.thetrain.storage.Transactions;
import com.github.davidcarboni.thetrain.storage.Website;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;
import java.nio.file.Path;

/**
 * API to commit an existing {@link Transaction}.
 */
@Api
public class Commit {

    @POST
    public Result commit(HttpServletRequest request,
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

            // Check for errors in the transaction
            if (transaction != null && transaction.hasErrors()) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                error = true;
                message = "This transaction cannot be committed because errors have been reported.";
            }

            // Get the website Path to publish to
            Path website = Website.path();
            if (website == null) {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                error = true;
                message = "Website folder could not be used: " + website;
            }

            // Commit
            if (!error) {
                boolean result = Publisher.commit(transaction, website);
                if (!result) {
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    error = true;
                    message = "Errors were detected in committing the transaction.";
                } else {
                    message = "Transaction committed.";
                    Transactions.listFiles(transaction);
                    System.out.println(message + " (" + transaction.id() + ")");
                }
            }

        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            error = true;
            message = ExceptionUtils.getStackTrace(e);
        }

        return new Result(message, error, transaction);
    }
}
