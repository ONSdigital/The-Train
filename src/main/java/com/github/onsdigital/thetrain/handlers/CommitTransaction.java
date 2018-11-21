package com.github.onsdigital.thetrain.handlers;

import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.logging.LogBuilder;
import com.github.onsdigital.thetrain.service.PublisherService;
import com.github.onsdigital.thetrain.service.TransactionsService;
import spark.Request;
import spark.Response;

import java.nio.file.Path;

import static com.github.onsdigital.thetrain.logging.LogBuilder.logBuilder;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Endpoint to commit an existing {@link Transaction}.
 */
public class CommitTransaction extends BaseHandler {

    private TransactionsService transactionsService;
    private PublisherService publisherService;

    public CommitTransaction(TransactionsService transactionsService, PublisherService publisherService) {
        this.transactionsService = transactionsService;
        this.publisherService = publisherService;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        Transaction transaction = null;
        LogBuilder log = logBuilder();

        try {
            // Transaction ID
            transaction = transactionsService.getTransaction(request);
            log.transactionID(transaction.id());

            // Get the website Path to publish to
            Path website = publisherService.websitePath();
            log.websitePath(website).info("request valid proceeding with committing transaction");

            boolean commitSuccessful = publisherService.commit(transaction, website);
            if (!commitSuccessful) {
                log.transactionID(transaction.id()).error("commit transaction was unsuccessful");
                throw new PublishException("commiting publish to website was unsuccessful", transaction);
            }

            // no errors return success response
            log.responseStatus(OK_200).info("commiting publish to website completed successfully");
            response.status(OK_200);
            return new Result("Transaction committed.", false, transaction);

        } finally {
            log.info("persisting changes to transaction");
            transactionsService.update(transaction);
        }
    }
}
