package com.github.onsdigital.thetrain.helpers;

import com.github.davidcarboni.encryptedfileupload.EncryptedFileItemFactory;
import com.github.onsdigital.thetrain.exception.BadRequestException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.onsdigital.thetrain.logging.TrainEvent.error;
import static com.github.onsdigital.thetrain.logging.TrainEvent.info;

public class FileUploadHelper {

    /**
     * Return an inputstream for an uploaded file. Caller is responsible for closing the returned stream.
     *
     * @param request The http request.
     * @return A temp file containing the file data.
     * @throws IOException If an error occurs in processing the file.
     */
    public InputStream getFileInputStream(HttpServletRequest request, String transactionID) throws BadRequestException {
        try {
            InputStream result = null;

            // Set up the objects that do all the heavy lifting
            EncryptedFileItemFactory factory = new EncryptedFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);

            // Read the items - this will save the values to temp files
            List<FileItem> fileItemList = upload.parseRequest(request);
            for (FileItem item : fileItemList) {
                if (!item.isFormField()) {
                    try {
                        info().data("name", item.getName())
                                .data("content_type", item.getContentType())
                                .data("field_name", item.getFieldName())
                                .log("identified fileItem in request body");
                        result = item.getInputStream();
                    } catch (IOException e) {
                        throw error().logException(e, "item.getInputStream() threw IO exception");
                    } catch (Exception e) {
                        throw error().logException(new BadRequestException(e, "", ""),"fileItem.getInputstream  an exception");
                    }
                }
            }

            if (result == null) {
                error().data("file_items", fileItemList.stream().map(f -> f.toString()).collect(Collectors.toList())).log("");
                throw new BadRequestException("expected request body but was null");
            }

            return result;
        } catch (IOException | FileUploadException e) {
            throw new BadRequestException(e, "error while attempting to get inputstream from request body", transactionID);
        }
    }
}
