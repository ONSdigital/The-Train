package com.github.onsdigital.thetrain.helpers.uploads;

import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.json.Transaction;
import spark.Request;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.nio.file.Path;

import static com.github.onsdigital.thetrain.logging.TrainEvent.info;

/**
 * Implementation of {@link CloseablePartSupplier} that retrieves a {@link CloseablePart} if it exists from a
 * {@link Request}.
 */
public class FilePartSupplier implements CloseablePartSupplier {

    static final String MULTIPART_CONFIG = "org.eclipse.jetty.multipartConfig";
    static final String FILE_PART_NAME = "file";

    private Path tmpDir;
    private long maxFileSize;
    private long maxRequestSize;
    private int fileThresholdSize;

    /**
     * Construct a new instance of {@link FilePartSupplier}.
     *
     * @param tmpDir            a {@link Path} to location temp files will be created.
     * @param maxFileSize       the maximum file upload size accepted by the API (in bytes). A value of -1 is unlimited.
     * @param maxRequestSize    the maximum request size accepted by the API (in bytes). A value of -1 is unlimited.
     * @param fileThresholdSize the threshold size for writing file uploads to temp files on disk (in MB). Uploads
     *                          smaller than this will be held in memory.
     */
    public FilePartSupplier(Path tmpDir, long maxFileSize, long maxRequestSize, int fileThresholdSize) {
        this.tmpDir = tmpDir;
        this.maxFileSize = maxFileSize;
        this.maxRequestSize = maxRequestSize;
        this.fileThresholdSize = fileThresholdSize;
    }

    @Override
    public CloseablePart getFilePart(Request req, Transaction t) throws PublishException, BadRequestException {
        if (req == null) {
            throw new PublishException("error getting file part from request as request was null");
        }

        if (t == null) {
            throw new PublishException("error getting file part from request transaction expected but was null");
        }

        HttpServletRequest raw = req.raw();
        if (raw == null) {
            throw new PublishException("error getting file part from request as HttpServletRequest was null", t);
        }

        req.attribute(MULTIPART_CONFIG,
                new MultipartConfigElement(tmpDir.toString(), maxFileSize, maxRequestSize, fileThresholdSize));

        info().transactionID(t).log("parsing request body for file item");
        Part part = null;
        try {
            part = raw.getPart(FILE_PART_NAME);
        } catch (Exception ex) {
            throw new BadRequestException(ex, "error attempting to retrieve multipart file upload request body", t.id());
        }

        if (part == null) {
            throw new BadRequestException("expected multipart file upload request body but was null", t.id());
        }

        return new CloseablePart(part, t.id());
    }
}