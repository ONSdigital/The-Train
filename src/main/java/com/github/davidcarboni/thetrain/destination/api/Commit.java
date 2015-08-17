package com.github.davidcarboni.thetrain.destination.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.destination.json.Result;
import com.github.davidcarboni.thetrain.destination.json.Transaction;
import com.github.davidcarboni.thetrain.destination.storage.Publisher;
import com.github.davidcarboni.thetrain.destination.storage.Transactions;
import com.github.davidcarboni.thetrain.destination.storage.Website;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.PUT;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by david on 30/07/2015.
 */
@Api
public class Commit {

    @PUT
    public Result commit(HttpServletRequest request,
                         HttpServletResponse response) throws IOException, FileUploadException {

        Transaction transaction = null;
        String message = null;
        boolean error = false;

        try {

            // Get the transaction
            String transactionId = request.getParameter("transactionId");
            if (StringUtils.isBlank(transactionId)) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                error = true;
                message = "Please provide a transactionId parameter.";
            }
            transaction = Transactions.get(transactionId);
            if (transaction == null) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                error = true;
                message = "Unknown transaction " + transactionId;
            }

            // Get the Path to the website folder we're going to publish to
            Path website = Website.path();
            if (website == null) {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                error = true;
                message = "Website folder could not be used: " + website;
            }

            // Commit
            if (!error) {
                Publisher.commit(transaction, website);
            }

        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            error = true;
            message = ExceptionUtils.getStackTrace(e);
        }

        return new Result(message, error, transaction);
    }
}
