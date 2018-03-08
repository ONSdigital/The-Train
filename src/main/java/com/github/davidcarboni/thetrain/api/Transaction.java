package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.json.Result;
import com.github.davidcarboni.thetrain.storage.Transactions;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

import static com.github.davidcarboni.thetrain.logging.LogBuilder.error;
import static com.github.davidcarboni.thetrain.logging.LogBuilder.info;
import static com.github.davidcarboni.thetrain.logging.LogBuilder.warn;

/**
 * API to query the details of an existing {@link com.github.davidcarboni.thetrain.json.Transaction Transaction}.
 */
@Api
public class Transaction {

    @GET
    public Result getTransactionDetails(HttpServletRequest request,
                                        HttpServletResponse response) throws IOException, FileUploadException {

        com.github.davidcarboni.thetrain.json.Transaction transaction = null;
        String message = null;
        boolean error = false;
        String transactionID = null;

        try {
            // Transaction ID
            transactionID = request.getParameter("transactionId");
            if (StringUtils.isBlank(transactionID)) {
                warn("transaction: transactionID required but none provided").log();
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                error = true;
                message = "Please provide a transactionId parameter.";
            }

            // Transaction object
            if (!error) {
                String encryptionPassword = request.getParameter("encryptionPassword");
                transaction = Transactions.get(transactionID, encryptionPassword);
                if (transaction == null) {
                    warn("transaction: transaction not found")
                            .transactionID(transactionID)
                            .log();
                    response.setStatus(HttpStatus.BAD_REQUEST_400);
                    error = true;
                    message = "Unknown transaction " + transactionID;
                } else {
                    message = "Details for transaction " + transaction.id();
                    Transactions.listFiles(transaction);
                }
            }
        } catch (Exception e) {
            error(e, "transaction: unexpected error while attempting to get transaction")
                    .transactionID(transactionID)
                    .log();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            error = true;
            message = ExceptionUtils.getStackTrace(e);
        }
        info("transaction: get transaction completed successfully")
                .transactionID(transactionID)
                .log();
        return new Result(message, error, transaction);
    }
}
