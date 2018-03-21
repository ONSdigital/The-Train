package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.api.common.Endpoint;
import com.github.davidcarboni.thetrain.json.Result;
import com.github.davidcarboni.thetrain.logging.Logger;
import com.github.davidcarboni.thetrain.storage.Transactions;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

import static com.github.davidcarboni.thetrain.logging.Logger.newLogger;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Endpoint to query the details of an existing {@link com.github.davidcarboni.thetrain.json.Transaction Transaction}.
 */
@Api
public class Transaction implements Endpoint {

    @GET
    public Result getTransactionDetails(HttpServletRequest request,
                                        HttpServletResponse response) throws IOException, FileUploadException {

        Logger logger = newLogger().endpoint(this);
        com.github.davidcarboni.thetrain.json.Transaction transaction = null;
        String transactionID = null;

        try {
            // Transaction ID
            transactionID = request.getParameter(TRANSACTION_ID_KEY);
            if (StringUtils.isBlank(transactionID)) {
                logger.responseStatus(BAD_REQUEST_400)
                        .warn("transactionID required but none provided");

                response.setStatus(BAD_REQUEST_400);
                return new Result("Please provide a transactionId parameter.", true, null);
            }

            logger.transactionID(transactionID);

            String encryptionPassword = request.getParameter(ENCRYPTION_PASSWORD_KEY);
            if (StringUtils.isEmpty(encryptionPassword)) {
                logger.responseStatus(BAD_REQUEST_400)
                        .warn("transaction requires encryptionPassword but none was provided");

                response.setStatus(BAD_REQUEST_400);
                return new Result("transaction requires encryptionPassword but none was provided", true, null);
            }

            transaction = Transactions.get(transactionID, encryptionPassword);
            if (transaction == null) {
                logger.responseStatus(BAD_REQUEST_400)
                        .warn("transaction with specified ID not found");

                response.setStatus(BAD_REQUEST_400);
                return new Result("Unknown transaction " + transactionID, true, null);
            }

            logger.responseStatus(OK_200)
                    .info("get transaction completed successfully");

            return new Result("Details for transaction " + transaction.id(), false, transaction);

        } catch (Exception e) {
            logger.responseStatus(INTERNAL_SERVER_ERROR_500)
                    .error(e, "unexpected error while attempting to get transaction");

            response.setStatus(INTERNAL_SERVER_ERROR_500);
            return new Result(ExceptionUtils.getStackTrace(e), true, transaction);
        }
    }
}
