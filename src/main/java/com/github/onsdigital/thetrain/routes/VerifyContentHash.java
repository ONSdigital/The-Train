package com.github.onsdigital.thetrain.routes;

import com.github.onsdigital.thetrain.json.JSONReader;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.json.VerifyHashEnity;
import com.github.onsdigital.thetrain.response.VerifyHashResult;
import com.github.onsdigital.thetrain.service.ContentException;
import com.github.onsdigital.thetrain.service.ContentService;
import com.github.onsdigital.thetrain.service.TransactionsService;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.logging.TrainEvent.error;
import static spark.Spark.halt;

public class VerifyContentHash extends BaseHandler {

    private TransactionsService transactionsService;
    private ContentService contentService;
    private JSONReader jsonReader;

    public VerifyContentHash(TransactionsService transactionsService, ContentService contentService,
                             JSONReader jsonReader) {
        this.transactionsService = transactionsService;
        this.contentService = contentService;
        this.jsonReader = jsonReader;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        Transaction trans = transactionsService.getTransaction(request);

        VerifyHashEnity entity = jsonReader.fromRequestBody(request, VerifyHashEnity.class);
        String uri = entity.getUri();
        String hash = entity.getHash();

        boolean isValid = verifyHash(trans, uri, hash);

        return new VerifyHashResult(trans.id(), uri, hash, isValid);
    }

    private boolean verifyHash(Transaction transaction, String uri, String hash) throws Exception {
        boolean validHashValue = false;

        try {
            validHashValue = contentService.isValidHash(transaction, uri, hash);
        } catch (Exception ex) {
            handleError(transaction, uri, ex);
        }

        return validHashValue;
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
