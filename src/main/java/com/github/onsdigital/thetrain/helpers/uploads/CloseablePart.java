package com.github.onsdigital.thetrain.helpers.uploads;

import com.github.onsdigital.thetrain.exception.PublishException;

import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static com.github.onsdigital.thetrain.logging.TrainEvent.error;
import static com.github.onsdigital.thetrain.logging.TrainEvent.info;

/**
 * Decorator class that extends a {@link Part} implementation adding a {@link CloseablePart#close()} method thereby
 * fulfilling the the {@link AutoCloseable} interface contract. <b>Does not change or alter the functionality of
 * underlying {@link Part}.</b>
 * <p>
 * <p>
 * Invoking {@link CloseablePart#close()} will call {@link Part#delete()} deleting/cleaning up any temp storage/files
 * used in handling the file upload request. By implementing the {@link AutoCloseable} inrerface this object can now
 * be used within a try-with-resources block taking advantage of the auto close functionality.
 */
public class CloseablePart implements Part, AutoCloseable {

    private Part wrappedPart;

    public CloseablePart(Part part) {
        this.wrappedPart = part;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return wrappedPart.getInputStream();
    }

    @Override
    public String getContentType() {
        return wrappedPart.getContentType();
    }

    @Override
    public String getName() {
        return wrappedPart.getName();
    }

    @Override
    public String getSubmittedFileName() {
        return wrappedPart.getSubmittedFileName();
    }

    @Override
    public long getSize() {
        return wrappedPart.getSize();
    }

    @Override
    public void write(String fileName) throws IOException {
        wrappedPart.write(fileName);
    }

    @Override
    public void delete() throws IOException {
        wrappedPart.delete();
    }

    @Override
    public String getHeader(String name) {
        return wrappedPart.getHeader(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return wrappedPart.getHeaders(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return wrappedPart.getHeaderNames();
    }

    @Override
    public void close() throws PublishException {
        try {
            if (this.wrappedPart != null) {
                info().log("attempting to clean up tmp file");
                this.wrappedPart.delete();
            }
        } catch (Exception ex) {
            // log error but don't throw
            error().exception(ex).log("error calling close on multiple part file upload part");
        }
    }
}
