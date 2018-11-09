package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.api.common.Endpoint;
import com.github.davidcarboni.thetrain.json.Result;
import com.github.davidcarboni.thetrain.logging.LogBuilder;
import com.github.davidcarboni.thetrain.service.TransactionsService;
import com.github.davidcarboni.thetrain.service.TransactionsServiceImpl;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

import static com.github.davidcarboni.thetrain.logging.LogBuilder.logBuilder;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Endpoint to query the details of an existing {@link com.github.davidcarboni.thetrain.json.Transaction Transaction}.
 */
@Api
public class Transaction extends Endpoint {

    @GET
    public Result getTransactionDetails(HttpServletRequest request,
                                        HttpServletResponse response) throws IOException, FileUploadException {

        LogBuilder log = logBuilder().endpoint(this);
        com.github.davidcarboni.thetrain.json.Transaction transaction = null;
        String transactionID = null;

        try {
            // Transaction ID
            transactionID = request.getParameter(TRANSACTION_ID_KEY);
            if (StringUtils.isBlank(transactionID)) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: transactionID required but none provided");

                response.setStatus(BAD_REQUEST_400);
                return new Result("Please provide a transactionId parameter.", true, null);
            }

            log.transactionID(transactionID);

            transaction = getTransactionsService().get(transactionID);
            if (transaction == null) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: transaction with specified ID not found");

                response.setStatus(BAD_REQUEST_400);
                return new Result("Unknown transaction " + transactionID, true, null);
            }

            log.responseStatus(OK_200)
                    .info("get transaction completed successfully");

            response.setStatus(OK_200);
            return new Result("Details for transaction " + transaction.id(), false, transaction);

        } catch (Exception e) {
            log.responseStatus(INTERNAL_SERVER_ERROR_500)
                    .error(e, "unexpected error while attempting to get transaction");

            response.setStatus(INTERNAL_SERVER_ERROR_500);
            return new Result(ExceptionUtils.getStackTrace(e), true, transaction);
        }
    }
}
