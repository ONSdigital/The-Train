package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.api.common.Endpoint;
import com.github.davidcarboni.thetrain.json.Result;
import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.davidcarboni.thetrain.logging.Logger;
import com.github.davidcarboni.thetrain.service.PublisherService;
import com.github.davidcarboni.thetrain.service.PublisherServiceImpl;
import com.github.davidcarboni.thetrain.service.TransactionsService;
import com.github.davidcarboni.thetrain.service.TransactionsServiceImpl;
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

import static com.github.davidcarboni.thetrain.logging.Logger.newLogger;

/**
 * Endpoint to commit an existing {@link Transaction}.
 */
@Api
public class Commit implements Endpoint {

    private TransactionsService transactionsService;
    private PublisherService publisherService;

    @POST
    public Result commit(HttpServletRequest request,
                         HttpServletResponse response) throws IOException, FileUploadException {
        Transaction transaction = null;
        String transactionId = null;

        Logger logger = newLogger().endpoint(this);

        try {

            // Transaction ID
            transactionId = request.getParameter(TRANSACTION_ID_KEY);
            if (StringUtils.isBlank(transactionId)) {
                logger.warn("transactionID required but none provided");
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                return new Result("Please provide a transactionId parameter.", true, transaction);
            }

            // Transaction object
            String encryptionPassword = request.getParameter(ENCRYPTION_PASSWORD_KEY);
            if (StringUtils.isEmpty(encryptionPassword)) {
                logger.transactionID(transactionId)
                        .warn("encryptionPassword required but none was provided");
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                return new Result("encryptionPassword required but was empty or null " + transactionId, true, null);
            }

            transaction = getTransactionsService().get(transactionId, encryptionPassword);
            if (transaction == null) {
                logger.warn("transaction could not be found");
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                return new Result("Unknown transaction " + transactionId, true, null);
            }

            // Check the transaction state
            if (!transaction.isOpen()) {
                logger.warn("unexpected error, transaction is closed");
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                return new Result("This transaction is closed.", true, transaction);
            }

            // Check for errors in the transaction
            if (transaction.hasErrors()) {
                logger.errors(transaction.errors()).warn("unexpected error, transaction has errors");
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                return new Result("This transaction cannot be committed because errors have been reported.", true, transaction);
            }

            // Get the website Path to publish to
            Path website = getPublisherService().websitePath();
            if (website == null) {
                logger.warn("transaction commit error - website path is null");
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                return new Result("website folder could not be used: " + website, true, transaction);
            }

            logger.websitePath(website).info("no errors encountered setting up transaction for commit, proceeding");

            boolean commitSuccessful = getPublisherService().commit(transaction, website);
            if (!commitSuccessful) {
                logger.warn("commiting publish to website was unsuccessful");
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                return new Result("Errors were detected in committing the transaction.", true, transaction);
            }

            // no errors return success response
            logger.info("commiting publish to website completed successfully");
            response.setStatus(HttpStatus.OK_200);
            return new Result("Transaction committed.", false, transaction);

        } catch (Exception e) {
            logger.error(e, "transaction returned unexpected error");
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            return new Result(ExceptionUtils.getStackTrace(e), true, transaction);
        } finally {
            logger.info("updating transaction");
            getTransactionsService().update(transaction);
        }
    }

    private TransactionsService getTransactionsService() {
        if (transactionsService == null) {
            transactionsService = new TransactionsServiceImpl();
        }
        return transactionsService;
    }

    private PublisherService getPublisherService() {
        if (publisherService == null) {
            publisherService = new PublisherServiceImpl();
        }
        return publisherService;
    }
}
