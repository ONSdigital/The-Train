package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.encryptedfileupload.EncryptedFileItemFactory;
import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.helpers.ShaInputStream;
import com.github.davidcarboni.thetrain.json.Result;
import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.davidcarboni.thetrain.storage.Publisher;
import com.github.davidcarboni.thetrain.storage.Transactions;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.zip.ZipInputStream;

import static com.github.davidcarboni.thetrain.logging.LogBuilder.error;
import static com.github.davidcarboni.thetrain.logging.LogBuilder.info;
import static com.github.davidcarboni.thetrain.logging.LogBuilder.warn;

/**
 * API to publish a file within an existing {@link Transaction}.
 */
@Api
public class Publish {

    @POST
    public Result addFile(HttpServletRequest request,
                          HttpServletResponse response) throws IOException, FileUploadException {

        Transaction transaction = null;
        String transactionID = null;
        String uri = null;
        String message = null;
        boolean isError = false;

        try {
            // Record the start time
            Date startDate = new Date();

            // Get the file first because request.getParameter will consume the body of the request:
            try (InputStream data = getFile(request)) {

                // Now get the parameters:
                transactionID = request.getParameter("transactionId");
                uri = request.getParameter("uri");
                String encryptionPassword = request.getParameter("encryptionPassword");

                // Validate parameters
                if (StringUtils.isBlank(transactionID)) {
                    warn("publish: publish requires transactionID but none way provided").log();
                    response.setStatus(HttpStatus.BAD_REQUEST_400);
                    isError = true;
                    message = "Please provide transactionId and uri parameters.";
                }

                if (StringUtils.isBlank(uri)) {
                    warn("publish: publish requires uri but none way provided")
                            .transactionID(transactionID)
                            .log();
                    response.setStatus(HttpStatus.BAD_REQUEST_400);
                    isError = true;
                    message = "Please provide transactionId and uri parameters.";
                }

                info("publish: beginning publish")
                        .transactionID(transactionID)
                        .addParameter("uri", uri)
                        .log();

                // Get the transaction
                transaction = Transactions.get(transactionID, encryptionPassword);
                if (transaction == null) {
                    warn("publish: transaction not fund")
                            .transactionID(transactionID)
                            .log();
                    response.setStatus(HttpStatus.BAD_REQUEST_400);
                    isError = true;
                    message = "Unknown transaction " + transactionID;
                }

                // Check the transaction state
                if (transaction != null && !transaction.isOpen()) {
                    warn("publish: unexpected error transaction is closed")
                            .transactionID(transactionID)
                            .log();
                    response.setStatus(HttpStatus.BAD_REQUEST_400);
                    isError = true;
                    message = "This transaction is closed.";
                }

                if (data == null) {
                    warn("commitManifest: data is null")
                            .transactionID(transactionID)
                            .addParameter("uri", uri)
                            .log();
                    response.setStatus(HttpStatus.BAD_REQUEST_400);

                    isError = true;
                    message = "No data found for published file.";
                }

                boolean zipped = BooleanUtils.toBoolean(request.getParameter("zip"));

                if (!isError) {
                    info("publish: no errors while setting up publish transaction proceeding")
                            .transactionID(transactionID)
                            .addParameter("uri", uri)
                            .log();

                    boolean published;
                    if (zipped) {
                        info("publish: unzipping file")
                                .transactionID(transactionID)
                                .addParameter("uri", uri)
                                .log();
                        try (ZipInputStream input = new ZipInputStream(new BufferedInputStream(data))) {
                            published = Publisher.addFiles(transaction, uri, input);
                            info("publish: unzip success")
                                    .transactionID(transactionID)
                                    .addParameter("uri", uri)
                                    .log();
                        }
                    } else {
                        info("publish: adding file to publish transaction")
                                .transactionID(transactionID)
                                .addParameter("uri", uri)
                                .log();
                        try (ShaInputStream input = new ShaInputStream(new BufferedInputStream(data))) {
                            published = Publisher.addFile(transaction, uri, input, startDate);
                            info("publish: file successfully added to publish transaction")
                                    .transactionID(transactionID)
                                    .addParameter("uri", uri)
                                    .log();
                        }
                    }

                    if (published) {
                        info("publish: file successfully published")
                                .transactionID(transactionID)
                                .addParameter("uri", uri)
                                .log();
                        message = "Published to " + uri;
                    } else {
                        warn("publish: error while publishing file")
                                .transactionID(transactionID)
                                .addParameter("uri", uri)
                                .log();
                        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                        isError = true;
                        message = "Sadly " + uri + " was not published.";
                    }

                    Transactions.tryUpdateAsync(transaction.id());
                }

            }
        } catch (Exception e) {
            error(e, "publish: error while attempting to publish file")
                    .transactionID(transactionID)
                    .addParameter("uri", uri)
                    .log();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            isError = true;
            message = ExceptionUtils.getStackTrace(e);
        }

        info("publish: published completed successfully")
                .transactionID(transactionID)
                .addParameter("uri", uri)
                .log();
        return new Result(message, isError, transaction);
    }


    /**
     * Handles reading the uploaded file.
     *
     * @param request The http request.
     * @return A temp file containing the file data.
     * @throws IOException If an error occurs in processing the file.
     */
    InputStream getFile(HttpServletRequest request)
            throws IOException {
        InputStream result = null;

        // Set up the objects that do all the heavy lifting
        EncryptedFileItemFactory factory = new EncryptedFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);

        try {
            // Read the items - this will save the values to temp files
            for (FileItem item : upload.parseRequest(request)) {
                if (!item.isFormField()) {
                    result = item.getInputStream();
                }
            }
        } catch (Exception e) {
            error(e, "error while getting item inputstream").log();
            // item.write throws a general Exception, so specialise it by wrapping with IOException
            throw new IOException("Error processing uploaded file", e);
        }

        return result;
    }
}
