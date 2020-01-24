package com.github.onsdigital.thetrain.routes;

import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.response.ContentHashEntity;
import com.github.onsdigital.thetrain.service.ContentException;
import com.github.onsdigital.thetrain.service.ContentService;
import com.github.onsdigital.thetrain.service.TransactionsService;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.configuration.AppConfiguration.ENABLE_VERIFY_PUBLISH_CONTENT;
import static com.github.onsdigital.thetrain.logging.TrainEvent.error;
import static com.github.onsdigital.thetrain.logging.TrainEvent.info;
import static spark.Spark.halt;

/**
 * {@link spark.Route} that returns the SHA-1 hash for file content of the requested URI if it exists within the
 * transaction.
 */
public class GetContentHash extends BaseHandler {

    private static final String FEATURE_DISABLED_MSG = "unable to process request as feature is not enabled " +
            "if this is incorrect please check your application configuration";

    private TransactionsService transactionsService;
    private ContentService contentService;
    private boolean isFeatureEnabled;

    /**
     * Construct a new GetContentHash route.
     *
     * @param transactionsService {@link TransactionsService} used to get the transaction specified in the request.
     * @param contentService      {@link ContentService} used to get the hash value of the request content file.
     * @param isFeatureEnabled    feature flag toggle to enable/disable this endpoint. If disabled returns 404.
     */
    public GetContentHash(TransactionsService transactionsService, ContentService contentService,
                          boolean isFeatureEnabled) {
        this.transactionsService = transactionsService;
        this.contentService = contentService;
        this.isFeatureEnabled = isFeatureEnabled;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        if (!isFeatureEnabled) {
            info().featureFlag(ENABLE_VERIFY_PUBLISH_CONTENT).log(FEATURE_DISABLED_MSG);
            halt(404);
        }

        Transaction trans = transactionsService.getTransaction(request);
        String uri = getURI(request);
        String hashValue = getContentHashValue(trans, uri);
        return new ContentHashEntity(trans.id(), uri, hashValue);
    }

    private String getContentHashValue(Transaction transaction, String uri) {
        String hashValue = "";

        try {
            hashValue = contentService.getContentHash(transaction, uri);
        } catch (Exception ex) {
            handleError(transaction, uri, ex);
        }

        return hashValue;
    }

    private void handleError(Transaction transaction, String uri, Exception ex) {
        int status = getErrorStatus(ex);

        error().transactionID(transaction)
                .data("uri", uri)
                .data("status", status)
                .exceptionAll(ex)
                .log("error verifying content hash");

        halt(status);
    }

    private int getErrorStatus(Exception ex) {
        int status;

        if (ex instanceof IllegalArgumentException) {
            status = 400;
        } else if (ex instanceof ContentException) {
            status = 404;
        } else {
            status = 500;
        }

        return status;
    }
}
