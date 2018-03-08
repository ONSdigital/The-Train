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

import static com.github.davidcarboni.thetrain.logging.LogBuilder.error;
import static com.github.davidcarboni.thetrain.logging.LogBuilder.info;
import static com.github.davidcarboni.thetrain.logging.LogBuilder.warn;

/**
 * API to commit an existing {@link Transaction}.
 */
@Api
public class Commit {

    @POST
    public Result commit(HttpServletRequest request,
                         HttpServletResponse response) throws IOException, FileUploadException {
        Transaction transaction = null;
        String transactionId = null;
        String message = null;
        boolean isError = false;

        try {

            // Transaction ID
            transactionId = request.getParameter("transactionId");
            if (StringUtils.isBlank(transactionId)) {
                warn("commit: transactionID required but none provided").log();
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                isError = true;
                message = "Please provide a transactionId parameter.";
            }

            // Transaction object
            String encryptionPassword = request.getParameter("encryptionPassword");
            transaction = Transactions.get(transactionId, encryptionPassword);
            if (transaction == null) {
                warn("commit: encryptionPassword required but none was provided")
                        .transactionID(transactionId)
                        .log();
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                isError = true;
                message = "Unknown transaction " + transactionId;
            }

            // Check the transaction state
            if (transaction != null && !transaction.isOpen()) {
                warn("commit: unexpected error, transaction is closed")
                        .transactionID(transactionId)
                        .log();
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                isError = true;
                message = "This transaction is closed.";
            }

            // Check for errors in the transaction
            if (transaction != null && transaction.hasErrors()) {
                warn("commit: unexpected error, transaction has errors")
                        .transactionID(transactionId)
                        .errors(transaction.errors())
                        .log();
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                isError = true;
                message = "This transaction cannot be committed because errors have been reported.";
            }

            // Get the website Path to publish to
            Path website = Website.path();
            if (website == null) {
                warn("commit: transaction commit error - website path is null")
                        .transactionID(transactionId)
                        .log();
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                isError = true;
                message = "website folder could not be used: " + website;
            }

            // Commit
            if (!isError) {
                info("commit: no errors encountered setting up transaction for commit, proceeding")
                        .transactionID(transactionId)
                        .websitePath(website)
                        .log();
                boolean result = Publisher.commit(transaction, website);
                if (!result) {
                    warn("commit: commiting publish to website was unsuccessful")
                            .transactionID(transactionId)
                            .websitePath(website)
                            .log();
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    isError = true;
                    message = "Errors were detected in committing the transaction.";
                } else {
                    info("commit: commiting publish to website completed successfully")
                            .transactionID(transactionId)
                            .websitePath(website)
                            .log();
                    message = "Transaction committed.";
                }
            }

        } catch (Exception e) {
            error(e, "commit: transaction returned unexpected error")
                    .transactionID(transactionId)
                    .log();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            isError = true;
            message = ExceptionUtils.getStackTrace(e);
        } finally {
            info("commit: updating transaction")
                    .transactionID(transactionId)
                    .log();
            Transactions.update(transaction);
        }

        info("commit: completed successfully")
                .transactionID(transactionId)
                .log();
        return new Result(message, isError, transaction);
    }
}
