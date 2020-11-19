package com.github.onsdigital.thetrain.helpers.uploads;

import com.github.onsdigital.thetrain.exception.PublishException;

import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static com.github.onsdigital.thetrain.logging.TrainEvent.error;
import static com.github.onsdigital.thetrain.logging.TrainEvent.info;

/**
 * Decorator class that extends a {@link Part} implementing the {@link AutoCloseable} interface. <b>Does not change
 * or alter the functionality of underlying {@link Part}.</b>
 * <p>
 * <p>
 * Invoking {@link CloseablePart#close()} will call {@link Part#delete()} deleting/cleaning up any temp storage/files
 * used in handling the file upload request. Implementing the {@link AutoCloseable} inrerface allows this object to
 * be used within a try-with-resources block providing a neat solution to ensure any resources created are cleaned up
 * automatically.
 */
public class CloseablePart implements Part, AutoCloseable {

    private Part part;
    private final String transactionID;

    /**
     * Construct a new CloseablePart instance from the {@link Part} provided.
     *
     * @param part the {@link Part} to decorate with {@link AutoCloseable} funcationality
     */
    public CloseablePart(Part part, String transactionID) {
        this.part = part;
        this.transactionID = transactionID;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return part.getInputStream();
    }

    @Override
    public String getContentType() {
        return part.getContentType();
    }

    @Override
    public String getName() {
        return part.getName();
    }

    @Override
    public String getSubmittedFileName() {
        return part.getSubmittedFileName();
    }

    @Override
    public long getSize() {
        return part.getSize();
    }

    @Override
    public void write(String fileName) throws IOException {
        part.write(fileName);
    }

    @Override
    public void delete() throws IOException {
        part.delete();
    }

    @Override
    public String getHeader(String name) {
        return part.getHeader(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return part.getHeaders(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return part.getHeaderNames();
    }

    /**
     * @return the raw {@link Part} used by this object.
     */
    public Part getPart() {
        return part;
    }

    /**
     * @return the publishing {@link com.github.onsdigital.thetrain.json.Transaction#id} this file upload part
     * belongs to.
     */
    public String getTransactionID() {
        return transactionID;
    }

    /**
     * Close method fulfulling the {@link AutoCloseable} interface allowing this object to be used within a
     * <i>try-with-resources</i> block.
     * <p>
     * If the decorated {@link Part} is null does nothing. Otherwise calls {@link Part#delete()} to clean up any temp
     * resources/storage used in handling the file upload.
     *
     * @throws PublishException
     */
    @Override
    public void close() throws PublishException {
        try {
            if (this.part != null) {
                info().log("attempting to clean up tmp file");
                this.part.delete();
            }
        } catch (Exception ex) {
            // log error but don't throw
            error().transactionID(transactionID)
                    .exception(ex).log("error calling close on multipart file upload part");
        }
    }
}