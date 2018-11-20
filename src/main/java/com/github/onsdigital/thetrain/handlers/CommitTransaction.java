package com.github.onsdigital.thetrain.handlers;

import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.exception.InternalServerError;
import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.logging.LogBuilder;
import com.github.onsdigital.thetrain.service.PublisherService;
import com.github.onsdigital.thetrain.service.TransactionsService;
import spark.Request;
import spark.Response;

import java.io.IOException;
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
            if (website == null) {
                log.error("website path was null");
                throw new InternalServerError("transaction commit error - website path is null", transaction.id());
            }

            log.websitePath(website).info("request valid proceeding with committing transaction");

            boolean commitSuccessful = publisherService.commit(transaction, website);
            if (!commitSuccessful) {
                log.transactionID(transaction.id()).error("commit transaction was unsuccessful");
                throw new InternalServerError("commiting publish to website was unsuccessful", transaction.id());
            }

            // no errors return success response
            log.responseStatus(OK_200).info("commiting publish to website completed successfully");
            response.status(OK_200);
            return new Result("Transaction committed.", false, transaction);

        } catch (IOException e) {
            throw new BadRequestException("commit transaction unsuccessful", e);
        } catch (Exception e) {
            log.error(e, "unexpected error while attempting to create new transaction");
            throw new InternalServerError(e);
        } finally {
            log.info("persisting changes to transaction");
            transactionsService.update(transaction);
        }
    }
}
