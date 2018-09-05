package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.api.common.Endpoint;
import com.github.davidcarboni.thetrain.json.Result;
import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.davidcarboni.thetrain.logging.LogBuilder;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

import static com.github.davidcarboni.thetrain.logging.LogBuilder.logBuilder;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Endpoint to start a new {@link Transaction}.
 */
@Api
public class Begin extends Endpoint {

    @POST
    public Result beginTransaction(HttpServletRequest request,
                                   HttpServletResponse response) throws IOException, FileUploadException {
        LogBuilder log = logBuilder().endpoint(this);
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
