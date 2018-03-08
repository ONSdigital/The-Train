package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.restolino.framework.Api;
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

import static com.github.davidcarboni.thetrain.logging.LogBuilder.error;
import static com.github.davidcarboni.thetrain.logging.LogBuilder.info;
import static com.github.davidcarboni.thetrain.logging.LogBuilder.warn;

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


        com.github.davidcarboni.thetrain.json.Transaction transaction = null;
        String transactionId = null;
        String message = null;
        boolean error = false;

        try {
            // Now get the parameters:
            transactionId = request.getParameter("transactionId");
            String encryptionPassword = request.getParameter("encryptionPassword");

            // Validate parameters
            if (StringUtils.isBlank(transactionId)) {
                warn("commitManifest: transactionID is required but none was provided").log();
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                error = true;
                message = "Please provide transactionId and uri parameters.";
            }

            info("start commit manifest process")
                    .transactionID(transactionId)
                    .log();

            // Get the transaction
            transaction = Transactions.get(transactionId, encryptionPassword);
            if (transaction == null) {
                warn("commitManifest: transaction not found")
                        .transactionID(transactionId)
                        .log();
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                error = true;
                message = "Unknown transaction " + transactionId;
            }

            // Check the transaction state
            if (transaction != null && !transaction.isOpen()) {
                warn("commitManifest: unexpected error transaction is closed")
                        .transactionID(transactionId)
                        .log();
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                error = true;
                message = "This transaction is closed.";
            }

            if (manifest == null) {
                warn("commitManifest: manifest is empty")
                        .transactionID(transactionId)
                        .log();
                response.setStatus(HttpStatus.BAD_REQUEST_400);

                error = true;
                message = "No manifest found for in this request.";
            }

            // Get the website Path to publish to
            Path websitePath = Website.path();
            if (websitePath == null) {
                warn("commitManifest: website path is null")
                        .transactionID(transactionId)
                        .log();
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                error = true;
                message = "website folder could not be used: " + websitePath;
            }

            if (!error) {
                info("commitManifest: copying manifest files to website and adding files to delete")
                        .transactionID(transactionId)
                        .websitePath(websitePath)
                        .log();
                int copied = Publisher.copyFiles(transaction, manifest, websitePath);
                int deleted = Publisher.addFilesToDelete(transaction, manifest);
                message = "Copied " + copied + " files.";
                message += " Deleted " + deleted + " files.";

                if (copied != manifest.getFilesToCopy().size()) {
                    warn("commitManifest: number of copied files does not match expected in manifest")
                            .transactionID(transactionId)
                            .websitePath(websitePath)
                            .addParameter("copied", copied)
                            .addParameter("expected", manifest.getFilesToCopy().size())
                            .log();
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    error = true;
                    message = "Move failed. Copied " + copied + " of " + manifest.getFilesToCopy().size();
                } else {
                    info("commitManifest: copying manifest files to website and adding files to delete completed successfully")
                            .transactionID(transactionId)
                            .websitePath(websitePath)
                            .addParameter("copied", copied)
                            .log();
                }
            }


        } catch (Exception e) {
            error(e, "commitManifest: returned unexpected error")
                    .transactionID(transactionId)
                    .log();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            error = true;
            message = ExceptionUtils.getStackTrace(e);
        } finally {
            info("commitManifest: updating transaction")
                    .transactionID(transactionId)
                    .log();
            Transactions.update(transaction);
        }

        info("commitManifest: completed successfully")
                .transactionID(transactionId)
                .log();
        return new Result(message, error, transaction);
    }
}
