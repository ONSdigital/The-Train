package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.helpers.DateConverter;
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
import java.util.Date;

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

        try {

            // Transaction ID
            String transactionId = request.getParameter("transactionId");
            if (StringUtils.isBlank(transactionId)) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                error = true;
                message = "Please provide a transactionId parameter.";
            }

            // Transaction object
            if (!error) {
                String encryptionPassword = request.getParameter("encryptionPassword");
                transaction = Transactions.get(transactionId, encryptionPassword);
                if (transaction == null) {
                    response.setStatus(HttpStatus.BAD_REQUEST_400);
                    error = true;
                    message = "Unknown transaction " + transactionId;
                } else {
                    message = "Details for transaction " + transaction.id();
                    Transactions.listFiles(transaction);
                }
            }

            System.out.println(message);

        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            error = true;
            message = ExceptionUtils.getStackTrace(e);
        }

        System.out.println(DateConverter.toString(new Date()) + " " + message);
        return new Result(message, error, transaction);
    }
}
