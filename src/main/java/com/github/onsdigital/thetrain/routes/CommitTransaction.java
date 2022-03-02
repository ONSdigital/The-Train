package com.github.onsdigital.thetrain.routes;

import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.service.PublisherService;
import com.github.onsdigital.thetrain.service.TransactionsService;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.logging.TrainEvent.error;
import static com.github.onsdigital.thetrain.logging.TrainEvent.info;
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

        try {
            transaction = transactionsService.getTransaction(request);

            if (transaction == null) {
                throw new PublishException(COMMIT_UNSUCCESSFUL_ERR);
            }
            boolean isSuccess = publisherService.commit(transaction);
            if (!isSuccess) {
                error().transactionID(transaction.id()).log(COMMIT_UNSUCCESSFUL_ERR);
                throw new PublishException(COMMIT_UNSUCCESSFUL_ERR, transaction);
            }
            // Check transaction in case an issue has been identified before attempting to commit
            if (transaction.hasErrors()) {
                transaction.setStatus(Transaction.COMMIT_FAILED);
                transaction.addError(COMMIT_UNSUCCESSFUL_ERR);
                PublishException publishException = new PublishException(COMMIT_UNSUCCESSFUL_ERR, transaction);
                error().transactionID(transaction.id())
                        .exception(publishException)
                        .log(COMMIT_UNSUCCESSFUL_ERR);
                throw publishException;
            }
            // No previous issues. Attempt to commit transaction and store success status
            transaction.setStatus(Transaction.COMMITTED);
            publisherService.commit(transaction);
            info().transactionID(transaction.id()).log(COMMIT_SUCCESSFUL_MSG);
            response.status(OK_200);
            return new Result(RESULT_SUCCESS_MSG, false, transaction);
        } finally {
            // transactionsService.update can handles a null transaction.
            transactionsService.update(transaction);
        }
    }
}
