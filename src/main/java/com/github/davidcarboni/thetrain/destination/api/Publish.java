package com.github.davidcarboni.thetrain.destination.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.destination.json.Transaction;
import com.github.davidcarboni.thetrain.destination.storage.Publisher;
import com.github.davidcarboni.thetrain.destination.storage.Transactions;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by david on 30/07/2015.
 */
@Api
public class Publish {

    @POST
    public String addFile(HttpServletRequest request,
                          HttpServletResponse response) throws IOException, FileUploadException {

        // Get the file first because request.getParameter will consume the body of the request:
        Path file = getFile(request);

        // Now get the parameters:
        String transactionId = request.getParameter("transactionId");
        String uri = request.getParameter("uri");

        // Validate
        if (StringUtils.isBlank(transactionId) || StringUtils.isBlank(uri)) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return "Please provide transactionId and uri parameters.";
        }
        Transaction transaction = Transactions.get(transactionId);
        if (transaction == null) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return "Unknown transaction " + transactionId;
        }

        // Publish
        String sha = Publisher.addFile(transaction, uri, file);
        if (StringUtils.isBlank(sha)) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            sha = "Sadly '"+uri+"' was not published.";
        }
        return sha;
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
        DiskFileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);

        try {
            // Read the items - this will save the values to temp files
            for (FileItem item : upload.parseRequest(request)) {
                if (!item.isFormField()) {
                    result = Files.createTempFile("upload", ".file");
                    item.write(result.toFile());
                }
            }
        } catch (Exception e) {
            // item.write throws a general Exception, so specialise it by wrapping with IOException
            throw new IOException("Error processing uploaded file", e);
        }

        return result;
    }
}
