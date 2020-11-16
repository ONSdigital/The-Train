package com.github.onsdigital.thetrain.helpers.uploads;

import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.exception.PublishException;
import spark.Request;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.nio.file.Path;

import static com.github.onsdigital.thetrain.logging.TrainEvent.info;

public class FileUploadHelper {

    static final String MULTIPART_CONFIG = "org.eclipse.jetty.multipartConfig";

    private Path tmpDir;

    public FileUploadHelper(Path tmpDir) {
        this.tmpDir = tmpDir;
    }

    public CloseablePart getUploadFilePart(Request req, String transactionID) throws BadRequestException,
            PublishException {
        MultipartConfigElement cfg = new MultipartConfigElement(tmpDir.toString(), -1L, -1L, 1024 * 1024 * 1);
        req.attribute(MULTIPART_CONFIG, cfg);

        info().transactionID(transactionID).log("parsing request body for file item");

        Part part = null;
        try {
            part = req.raw().getPart("file");
        } catch (Exception ex) {
            throw new BadRequestException("error attempting to retriving multipart upload request body");
        }

        if (part == null) {
            throw new BadRequestException("expected multipart upload request body but was null");
        }

        return new CloseablePart(part);
    }
}
