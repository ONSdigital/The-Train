package com.github.onsdigital.thetrain.routes;

import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.service.PublisherService;
import com.github.onsdigital.thetrain.service.TransactionsService;
import spark.Request;
import spark.Response;
import org.apache.http.HttpStatus;

import static com.github.onsdigital.thetrain.logging.TrainEvent.error;
import static com.github.onsdigital.thetrain.logging.TrainEvent.info;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static java.lang.String.format;

/**
 * {@link spark.Route} implementation for handling commit publishing transaction requests.
 */
public class CommitTransaction extends BaseHandler {

    static final String COMMIT_UNSUCCESSFUL_ERR = "committing publish to website was unsuccessful";
    static final String COMMIT_UNSUCCESSFUL_ID_ERR = COMMIT_UNSUCCESSFUL_ERR + " for transaction:%s";
    static final String COMMIT_SUCCESSFUL_MSG = "committing publish to website completed successfulLY";
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
            //check transaction in case an issue has been identified before attempting to commit
            if (transaction.hasErrors() || (transaction.getStatus() != null && !transaction.getStatus().equals(Transaction.PUBLISHING))) {
                transaction.setStatus(Transaction.COMMIT_FAILED);
                transaction.addError(format(COMMIT_UNSUCCESSFUL_ID_ERR, transaction.id()));
                error().transactionID(transaction.id()).log(COMMIT_UNSUCCESSFUL_ERR);
                //Throwing here is not ideal, as a Publishing Exception will be wrapped in another Publish exception, however better for readability.
                throw new PublishException(COMMIT_UNSUCCESSFUL_ERR, transaction);
            } else {
                //if no previous issues, attempt to commit transaction and store success status
                publisherService.commit(transaction);
            }

            info().transactionID(transaction.id()).log(COMMIT_SUCCESSFUL_MSG);
            response.status(OK_200);
            transaction.setStatus(Transaction.COMMITTED);
            return new Result(RESULT_SUCCESS_MSG, false, transaction);
        } catch (PublishException e) {
            if (transaction!=null) {
                transaction.setStatus(Transaction.COMMIT_FAILED);
                transaction.addError(format(COMMIT_UNSUCCESSFUL_ID_ERR,transaction));
                throw new PublishException(COMMIT_UNSUCCESSFUL_ERR, e, transaction, HttpStatus.SC_INTERNAL_SERVER_ERROR);
            } else {
                throw new PublishException(COMMIT_UNSUCCESSFUL_ERR);
            }
        } finally {
            //transactionsService.update can accommodate a null transaction.
            transactionsService.update(transaction);
        }
    }
}
