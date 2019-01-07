package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.api.common.Endpoint;
import com.github.davidcarboni.thetrain.json.Result;
import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.davidcarboni.thetrain.logging.LogBuilder;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;
import java.nio.file.Path;

import static com.github.davidcarboni.thetrain.logging.LogBuilder.logBuilder;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Endpoint to commit an existing {@link Transaction}.
 */
@Api
public class Commit extends Endpoint {

    @POST
    public Result commit(HttpServletRequest request,
                         HttpServletResponse response) throws IOException, FileUploadException {
        Transaction transaction = null;
        String transactionId = null;

        LogBuilder log = logBuilder().endpoint(this);

        try {
            // Transaction ID
            transactionId = request.getParameter(TRANSACTION_ID_KEY);
            if (StringUtils.isBlank(transactionId)) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: transactionID required but none provided");
                response.setStatus(BAD_REQUEST_400);
                return new Result("Please provide a transactionId parameter.", true, transaction);
            }

            log.transactionID(transactionId);

            // Transaction object
            transaction = getTransactionsService().get(transactionId);
            if (transaction == null) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: transaction was not found");
                response.setStatus(BAD_REQUEST_400);
                return new Result("Unknown transaction " + transactionId, true, null);
            }

            // Check the transaction state
            if (!transaction.isOpen()) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("unexpected error, transaction is closed");
                response.setStatus(BAD_REQUEST_400);
                return new Result("This transaction is closed.", true, transaction);
            }

            // Check for errors in the transaction
            if (transaction.hasErrors()) {
                log.responseStatus(BAD_REQUEST_400)
                        .errors(transaction.errors()).warn("bad request: transaction has errors");
                response.setStatus(BAD_REQUEST_400);
                return new Result("This transaction cannot be committed because errors have been reported.", true, transaction);
            }

            // Get the website Path to publish to
            Path website = getPublisherService().websitePath();
            if (website == null) {
                log.responseStatus(INTERNAL_SERVER_ERROR_500)
                        .warn("transaction commit error - website path is null");
                response.setStatus(INTERNAL_SERVER_ERROR_500);
                return new Result("website folder could not be used: " + website, true, transaction);
            }

            log.websitePath(website).info("request valid proceeding with committing transaction");

            boolean commitSuccessful = getPublisherService().commit(transaction, website);
            if (!commitSuccessful) {
                log.responseStatus(INTERNAL_SERVER_ERROR_500)
                        .warn("commiting publish to website was unsuccessful");
                response.setStatus(INTERNAL_SERVER_ERROR_500);
                return new Result("Errors were detected in committing the transaction.", true, transaction);
            }

            // no errors return success response
            log.responseStatus(OK_200)
                    .info("commiting publish to website completed successfully");
            response.setStatus(OK_200);
            return new Result("Transaction committed.", false, transaction);

        } catch (Exception e) {
            log.responseStatus(INTERNAL_SERVER_ERROR_500)
                    .error(e, "transaction returned unexpected error");
            response.setStatus(INTERNAL_SERVER_ERROR_500);
            return new Result(ExceptionUtils.getStackTrace(e), true, transaction);
        } finally {
            log.info("persisting changes to transaction");
            getTransactionsService().update(transaction);
        }
    }

}
