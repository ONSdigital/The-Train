package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.api.common.Endpoint;
import com.github.davidcarboni.thetrain.helpers.Hash;
import com.github.davidcarboni.thetrain.helpers.PathUtils;
import com.github.davidcarboni.thetrain.json.FileHash;
import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.davidcarboni.thetrain.logging.LogBuilder;
import com.github.davidcarboni.thetrain.storage.Website;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.davidcarboni.thetrain.logging.LogBuilder.logBuilder;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;

/**
 * Endpoint to start a new {@link Transaction}.
 */
@Api
public class Verify extends Endpoint {

    @GET
    public FileHash verify(HttpServletRequest request,
                           HttpServletResponse response) throws IOException, FileUploadException {
        LogBuilder log = logBuilder().endpoint(this);
        FileHash result = new FileHash();

        try {
            // Get the parameters:
            result.uri = request.getParameter(URI_KEY);
            String sha1 = request.getParameter(SHA1_KEY);

            // Validate parameters
            if (StringUtils.isBlank(result.uri)) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: uri is required but none provided");

                response.setStatus(BAD_REQUEST_400);
                result.error = true;
                result.message = "Please provide uri and sha1 parameters.";
                return result;
            }

            log.uri(result.uri);

            if (StringUtils.isBlank(sha1)) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: verify: sha1 is required but none provided");
                response.setStatus(BAD_REQUEST_400);

                result.error = true;
                result.message = "Please provide uri and sha1 parameters.";
                return result;
            }

            Path path = PathUtils.toPath(result.uri, Website.path());
            if (!Files.exists(path)) {
                log.websitePath(Website.path())
                        .responseStatus(BAD_REQUEST_400)
                        .warn("bad request: verify: file does not exist in website destination");

                result.message = "File does not exist in the destination: " + result.uri;
                return result;
            }

            result.sha1 = Hash.sha(path);
            result.matched = StringUtils.equals(sha1, result.sha1);
            result.message = "SHA matched: " + result.matched;
            if (!result.matched) {
                result.message += " (" + sha1 + " -> " + result.sha1 + ")";
            }

        } catch (Exception e) {
            log.responseStatus(INTERNAL_SERVER_ERROR_500)
                    .error(e, "verify: unexpected error verifing");

            response.setStatus(INTERNAL_SERVER_ERROR_500);
            result.error = true;
            result.message = ExceptionUtils.getStackTrace(e);
        }
        return result;
    }
}
