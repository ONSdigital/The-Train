package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.api.common.Endpoint;
import com.github.davidcarboni.thetrain.json.Result;
import com.github.davidcarboni.thetrain.json.request.Manifest;
import com.github.davidcarboni.thetrain.logging.LogBuilder;
import com.github.davidcarboni.thetrain.storage.Publisher;
import com.github.davidcarboni.thetrain.storage.Transactions;
import com.github.davidcarboni.thetrain.storage.Website;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;
import java.nio.file.Path;

import static com.github.davidcarboni.thetrain.logging.LogBuilder.logBuilder;
import static java.lang.String.format;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Endpoint to move files within an existing {@link com.github.davidcarboni.thetrain.json.Transaction}.
 */
@Api
public class CommitManifest extends Endpoint {
    @POST
    public Result commitManifest(
            HttpServletRequest request,
            HttpServletResponse response,
            Manifest manifest
    ) throws IOException, FileUploadException {

        com.github.davidcarboni.thetrain.json.Transaction transaction = null;
        String transactionId = null;
        LogBuilder log = logBuilder().endpoint(this);

        try {
            // Now get the parameters:
            transactionId = request.getParameter(TRANSACTION_ID_KEY);

            // Validate parameters
            if (StringUtils.isBlank(transactionId)) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: transactionID is required but none was provided");
                response.setStatus(BAD_REQUEST_400);
                return new Result("Please provide transactionId and uri parameters.", true, null);
            }

            // add the transactionID to the log parameters.
            log.transactionID(transactionId);

            log.info("request valid starting commit manifest for transaction");

            // Get the transaction
            transaction = Transactions.get(transactionId);
            if (transaction == null) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: transaction with specified id was not found");
                response.setStatus(BAD_REQUEST_400);
                return new Result("Unknown transaction " + transactionId, true, null);
            }

            // Check the transaction state
            if (transaction != null && !transaction.isOpen()) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: could not proceed as transaction is unexpectedly closed");
                response.setStatus(BAD_REQUEST_400);
                return new Result("This transaction is closed.", true, transaction);
            }

            if (manifest == null) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: unexpected error transaction manifest is empty");
                response.setStatus(BAD_REQUEST_400);
                return new Result("No manifest found for in this request.", true, transaction);
            }

            // Get the website Path to publish to
            Path websitePath = Website.path();
            if (websitePath == null) {
                log.responseStatus(INTERNAL_SERVER_ERROR_500)
                        .warn("unexpected error website path is null");
                response.setStatus(INTERNAL_SERVER_ERROR_500);
                return new Result("website folder could not be used: " + websitePath, true, transaction);
            }


            log.websitePath(websitePath).info("copying manifest files to website and adding files to delete");

            int copied = Publisher.copyFiles(transaction, manifest, websitePath);
            int deleted = Publisher.addFilesToDelete(transaction, manifest);

            if (copied != manifest.getFilesToCopy().size()) {
                log.responseStatus(INTERNAL_SERVER_ERROR_500)
                        .addParameter("actualCopies", copied)
                        .addParameter("expectedCopies", manifest.getFilesToCopy().size())
                        .warn("the number of copied files does not match expected in value of the manifest");

                response.setStatus(INTERNAL_SERVER_ERROR_500);
                return new Result("Move failed. Copied " + copied + " of " + manifest.getFilesToCopy().size(), true, transaction);
            }

            // success
            log.responseStatus(OK_200)
                    .addParameter("copied", copied)
                    .addParameter("deleted", deleted)
                    .info("copying manifest files to website and adding files to delete completed successfully");

            response.setStatus(OK_200);
            return new Result(format("Copied %d files. Deleted %s files.", copied, deleted), false, transaction);

        } catch (Exception e) {
            log.responseStatus(INTERNAL_SERVER_ERROR_500).error(e, "unexpected error");
            response.setStatus(INTERNAL_SERVER_ERROR_500);
            return new Result(ExceptionUtils.getStackTrace(e), true, transaction);
        } finally {
            log.info("updating transaction");
            try {
                Transactions.update(transaction);
            } catch (Exception e) {
                log.responseStatus(INTERNAL_SERVER_ERROR_500).error(e, "unexpected error while updating transaction");
                response.setStatus(INTERNAL_SERVER_ERROR_500);
                new Result("unexpected error while updating transaction", true, transaction);
            }
        }
    }
}
