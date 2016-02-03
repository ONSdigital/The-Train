package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.helpers.DateConverter;
import com.github.davidcarboni.thetrain.json.Result;
import com.github.davidcarboni.thetrain.json.request.Manifest;
import com.github.davidcarboni.thetrain.storage.Publisher;
import com.github.davidcarboni.thetrain.storage.Transactions;
import com.github.davidcarboni.thetrain.storage.Website;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;

/**
 * API to move files within an existing {@link com.github.davidcarboni.thetrain.json.Transaction}.
 */
@Api
public class CommitManifest {
    @POST
    public Result commitManifest(
            HttpServletRequest request,
            HttpServletResponse response,
            Manifest manifest
    ) throws IOException, FileUploadException {

        System.out.println(DateConverter.toString(new Date()) + " Start move");

        com.github.davidcarboni.thetrain.json.Transaction transaction = null;
        String message = null;
        boolean error = false;

        try {
            // Now get the parameters:
            String transactionId = request.getParameter("transactionId");
            String encryptionPassword = request.getParameter("encryptionPassword");

            // Validate parameters
            if (StringUtils.isBlank(transactionId)) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                error = true;
                message = "Please provide transactionId and uri parameters.";
            }

            // Get the transaction
            transaction = Transactions.get(transactionId, encryptionPassword);
            if (transaction == null) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                error = true;
                message = "Unknown transaction " + transactionId;
            }

            // Check the transaction state
            if (transaction != null && !transaction.isOpen()) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                error = true;
                message = "This transaction is closed.";
            }

            if (manifest == null) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);

                error = true;
                message = "No move manifest found for in this request.";
            }

            // Get the website Path to publish to
            Path websitePath = Website.path();
            if (websitePath == null) {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                error = true;
                message = "Website folder could not be used: " + websitePath;
            }

            if (!error) {
                int moved = Publisher.moveFiles(transaction, manifest, websitePath);
                message = "Moved " + moved + " files.";

                if (moved != manifest.moves.size()) {

                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    error = true;
                    message = "Move failed.";
                }
            }


        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            error = true;
            message = ExceptionUtils.getStackTrace(e);
        } finally {
            Transactions.update(transaction);
        }

        System.out.println(DateConverter.toString(new Date()) + " " + message + (transaction != null ? " (transaction " + transaction.id() + ")" : ""));
        return new Result(message, error, transaction);
    }
}
