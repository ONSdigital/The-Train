package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.encryptedfileupload.EncryptedFileItemFactory;
import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.api.common.Endpoint;
import com.github.davidcarboni.thetrain.helpers.ShaInputStream;
import com.github.davidcarboni.thetrain.json.Result;
import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.davidcarboni.thetrain.logging.LogBuilder;
import com.github.davidcarboni.thetrain.storage.Publisher;
import com.github.davidcarboni.thetrain.storage.Transactions;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.zip.ZipInputStream;

import static com.github.davidcarboni.thetrain.logging.LogBuilder.logBuilder;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Endpoint to publish a file within an existing {@link Transaction}.
 */
@Api
public class Publish extends Endpoint {

    @POST
    public Result addFile(HttpServletRequest request,
                          HttpServletResponse response) throws IOException, FileUploadException {

        LogBuilder log = logBuilder().endpoint(this);
        Transaction transaction = null;
        String transactionID = null;
        String encryptionPassword = null;
        String uri = null;

        try {
            // Record the start time
            Date startDate = new Date();

            // Get the file first because request.getParameter will consume the body of the request:
            try (InputStream data = getFile(request)) {

                transactionID = request.getParameter(TRANSACTION_ID_KEY);
                if (StringUtils.isBlank(transactionID)) {
                    log.responseStatus(BAD_REQUEST_400)
                            .warn("bad request: publish requires transactionID but none provided");

                    response.setStatus(BAD_REQUEST_400);
                    return new Result("Please provide transactionId and uri parameters.", true, null);
                }

                log.transactionID(transactionID);

                uri = request.getParameter(URI_KEY);
                if (StringUtils.isBlank(uri)) {
                    log.responseStatus(BAD_REQUEST_400)
                            .warn("bad request: publish requires uri but none provided");

                    response.setStatus(BAD_REQUEST_400);
                    return new Result("Please provide transactionId and uri parameters.", true, null);
                }

                log.uri(uri);

                // Get the transaction
                transaction = Transactions.get(transactionID);
                if (transaction == null) {
                    log.responseStatus(BAD_REQUEST_400)
                            .warn("bad request: no transaction with specified ID was found");

                    response.setStatus(BAD_REQUEST_400);
                    return new Result("Unknown transaction " + transactionID, true, null);
                }

                // Check the transaction state
                if (transaction != null && !transaction.isOpen()) {
                    log.responseStatus(BAD_REQUEST_400)
                            .warn("bad request: unexpected error transaction is closed");

                    response.setStatus(BAD_REQUEST_400);
                    return new Result("This transaction is closed.", true, transaction);
                }

                if (data == null) {
                    log.responseStatus(BAD_REQUEST_400)
                            .warn("bad request: unexpected error  data is null");
                    response.setStatus(BAD_REQUEST_400);
                    return new Result("No data found for published file.", true, transaction);
                }

                log.info("request valid proceeding with addFile");

                boolean zipped = BooleanUtils.toBoolean(request.getParameter(ZIP_KEY));
                boolean published;

                if (zipped) {
                    log.info("unzipping file");

                    try (ZipInputStream input = new ZipInputStream(new BufferedInputStream(data))) {
                        published = Publisher.addFiles(transaction, uri, input);
                        log.info("file unzupped and added successfully");
                    } catch (Exception e) {
                        log.error(e, "unexpected error while attempting to add zipped file to publish transaction");
                        throw e;
                    }
                } else {
                    log.info("adding file to publish transaction");
                    try (ShaInputStream input = new ShaInputStream(new BufferedInputStream(data))) {
                        published = Publisher.addFile(transaction, uri, input, startDate);
                        log.info("publish: file successfully added to publish transaction");
                    } catch (Exception e) {
                        log.error(e, "unexpected error while attempting to add file to publish transaction");
                        throw e;
                    }
                }

                if (published) {
                    log.responseStatus(OK_200).info("file added to  publish transaction successfully");

                } else {
                    log.responseStatus(INTERNAL_SERVER_ERROR_500)
                            .warn("error while adding file to publish transaction");

                    response.setStatus(INTERNAL_SERVER_ERROR_500);
                    return new Result("Sadly " + uri + " was not published.", true, transaction);
                }

                try {
                    Transactions.tryUpdateAsync(transaction.id());
                } catch (Exception e) {
                    log.responseStatus(INTERNAL_SERVER_ERROR_500)
                            .error(e, "unexpected error while attempting to async update transaction");
                    return new Result("unexpected error while attempting to async update transaction", true, transaction);
                }

                // otherwise its a successful response/
                return new Result("Published to " + uri, false, transaction);
            }

        } catch (Exception e) {
            log.responseStatus(INTERNAL_SERVER_ERROR_500)
                    .error(e, "error while attempting to add file to publish transaction");
            response.setStatus(INTERNAL_SERVER_ERROR_500);
            return new Result(ExceptionUtils.getStackTrace(e), true, transaction);
        }
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
            logBuilder().error(e, "error while getting item inputstream");
            // item.write throws a general Exception, so specialise it by wrapping with IOException
            throw new IOException("Error processing uploaded file", e);
        }

        return result;
    }
}
