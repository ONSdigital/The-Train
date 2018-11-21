package com.github.onsdigital.thetrain.routes;

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
 * {@link spark.Route} implementation for handling commit publishing transaction requests.
 */
public class CommitTransaction extends BaseHandler {

    static final String COMMIT_UNSUCCESSFUL_ERR = "commiting publish to website was unsuccessful";
    static final String COMMIT_SUCCESSFUL_MSG = "commiting publish to website completed successfulLY";
    static final String REQUEST_VALID_MSG = "request valid proceeding with committing transaction";
    static final String RESULT_SUCCESS_MSG = "Transaction committed.";

    private TransactionsService transactionsService;
    private PublisherService publisherService;

    /**
     * Ccnstruct a new Commit transaction {@link spark.Route}.
     *
     * @param transactionsService the {@link TransactionsService} to use.
     * @param publisherService    the {@link PublisherService} to use.
     */
    public CommitTransaction(TransactionsService transactionsService, PublisherService publisherService) {
        this.transactionsService = transactionsService;
        this.publisherService = publisherService;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        Transaction transaction = null;
        LogBuilder log = logBuilder();

        try {
            transaction = transactionsService.getTransaction(request);
            log.transactionID(transaction.id());

            Path website = publisherService.websitePath();
            log.websitePath(website).info(REQUEST_VALID_MSG);

            boolean isSuccess = publisherService.commit(transaction, website);
            if (!isSuccess) {
                log.transactionID(transaction.id()).error(COMMIT_UNSUCCESSFUL_ERR);
                throw new PublishException(COMMIT_UNSUCCESSFUL_ERR, transaction);
            }

            log.responseStatus(OK_200).info(COMMIT_SUCCESSFUL_MSG);
            response.status(OK_200);
            return new Result(RESULT_SUCCESS_MSG, false, transaction);

        } finally {
            transactionsService.update(transaction);
        }
    }
}
