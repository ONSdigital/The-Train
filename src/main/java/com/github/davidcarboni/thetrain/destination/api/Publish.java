package com.github.davidcarboni.thetrain.destination.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.destination.json.Result;
import com.github.davidcarboni.thetrain.destination.json.Transaction;
import com.github.davidcarboni.thetrain.destination.json.Uri;
import com.github.davidcarboni.thetrain.destination.storage.Publisher;
import com.github.davidcarboni.thetrain.destination.storage.Transactions;
import org.apache.commons.fileupload.DefaultFileItemFactory;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

/**
 * Created by david on 30/07/2015.
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
            Path file = getFile(request);

            // Now get the parameters:
            String transactionId = request.getParameter("transactionId");
            String uriString = request.getParameter("uri");

            // Validate
            if (StringUtils.isBlank(transactionId) || StringUtils.isBlank(uriString)) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                error = true;
                message = "Please provide transactionId and uri parameters.";
            }
            transaction = Transactions.get(transactionId);
            if (transaction == null) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                error = true;
                message = "Unknown transaction " + transactionId;
            }

            if (!error) {
                // Publish
                Uri uri = new Uri(uriString, startDate);
                String sha = Publisher.addFile(transaction, uri, file);
                if (StringUtils.isNotBlank(sha)) {
                    message = sha;
                } else {
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    error = true;
                    message = "Sadly '" + uri + "' was not published.";
                }
            }

        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            error = true;
            message = ExceptionUtils.getStackTrace(e);
        }

        // FIXME This is causing the error
        Result returnResult = new Result(message, error, transaction);

        return returnResult;
    }


    /**
     * Handles reading the uploaded file.
     *
     * @param request The http request.
     * @return A temp file containing the file data.
     * @throws IOException If an error occurs in processing the file.
     */
    Path getFile(HttpServletRequest request)
            throws IOException {
        Path result = null;

        // Set up the objects that do all the heavy lifting
        DiskFileItemFactory factory = new DefaultFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);

        try {
            // Read the items - this will save the values to temp files
            for (FileItem item : upload.parseRequest(request)) {
                if (!item.isFormField()) {
                    // Write file to local temp file
                    result = Files.createTempFile("upload", ".file");
                    item.write(result.toFile());

                    // Move file to Transaction folder


                    System.out.println("writing " + result.toString());
                }
            }
        } catch (Exception e) {
            // item.write throws a general Exception, so specialise it by wrapping with IOException
            throw new IOException("Error processing uploaded file", e);
        }

        return result;
    }
}
