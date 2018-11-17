package com.github.onsdigital.thetrain.api;

import com.github.onsdigital.thetrain.api.common.Endpoint;
import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.logging.LogBuilder;
import org.apache.commons.fileupload.FileUploadException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Endpoint to start a new {@link com.github.onsdigital.thetrain.json.Transaction}.
 */
//@Api
public class Begin extends Endpoint {

    //@POST
    public Result beginTransaction(HttpServletRequest request,
                                   HttpServletResponse response) throws IOException, FileUploadException {
        LogBuilder log = LogBuilder.logBuilder().endpoint(this);
        Transaction transaction = null;

        try {
            log.info("creating new publishing transaction");
            transaction = getTransactionsService().create();
            log.transactionID(transaction.id()).info("transaction created successfully");

            response.setStatus(OK_200);
            return new Result("New transaction created.", false, transaction);

        } catch (IOException e) {
            response.setStatus(INTERNAL_SERVER_ERROR_500);
            log.error(e, "unexpected error while attempting to create transaction");
            return new Result("beingTransaction encountered unexpected error", true, transaction);
        }
    }
}
