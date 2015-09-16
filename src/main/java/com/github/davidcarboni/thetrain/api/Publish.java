package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.encryptedfileupload.EncryptedFileItemFactory;
import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.helpers.DateConverter;
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

/**
 * API to publish a file within an existing {@link Transaction}.
 */
@Api
public class Publish {

    @POST
    public Result addFile(HttpServletRequest request,
                          HttpServletResponse response) throws IOException, FileUploadException {

        Transaction transaction = null;
        String message = null;
        boolean error = false;

        try {
            // Record the start time
            Date startDate = new Date();

            // Get the file first because request.getParameter will consume the body of the request:
            try (InputStream data = getFile(request)) {

                // Now get the parameters:
                String transactionId = request.getParameter("transactionId");
                String uri = request.getParameter("uri");
                String encryptionPassword = request.getParameter("encryptionPassword");

                // Validate parameters
                if (StringUtils.isBlank(transactionId) || StringUtils.isBlank(uri)) {
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

                if (data == null) {
                    response.setStatus(HttpStatus.BAD_REQUEST_400);

                    error = true;
                    message = "No data found for published file.";
                }

                boolean zipped = BooleanUtils.toBoolean(request.getParameter("zip"));

                if (!error) {
                    // Publish
                    boolean published;
                    if (zipped) {
                        try (ZipInputStream input = new ZipInputStream(new BufferedInputStream(data))) {
                            published = Publisher.addFiles(transaction, uri, input);
                        }
                    } else {
                        try (InputStream input = new BufferedInputStream(data)) {
                            published = Publisher.addFile(transaction, uri, input, startDate);
                        }
                    }

                    if (published) {
                        message = "Published to " + uri;
                    } else {
                        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                        error = true;
                        message = "Sadly " + uri + " was not published.";
                    }
                }

            }
        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            error = true;
            message = ExceptionUtils.getStackTrace(e);
        }

        System.out.println(DateConverter.toString(new Date()) + " " + message + (transaction != null ? " (transaction " + transaction.id() + ")" : ""));
        return new Result(message, error, transaction);
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
            // item.write throws a general Exception, so specialise it by wrapping with IOException
            throw new IOException("Error processing uploaded file", e);
        }

        return result;
    }
}
