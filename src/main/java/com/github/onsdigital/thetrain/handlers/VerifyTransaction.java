package com.github.onsdigital.thetrain.handlers;

import com.github.onsdigital.thetrain.helpers.Hash;
import com.github.onsdigital.thetrain.helpers.PathUtils;
import com.github.onsdigital.thetrain.json.FileHash;
import com.github.onsdigital.thetrain.logging.LogBuilder;
import com.github.onsdigital.thetrain.storage.Website;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import spark.Request;
import spark.Response;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.onsdigital.thetrain.logging.LogBuilder.logBuilder;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;

public class VerifyTransaction extends BaseHandler {

    @Override
    public Object handle(Request request, Response response) throws Exception {
        LogBuilder log = logBuilder();
        FileHash result = new FileHash();

        try {
            // Get the parameters:
            result.uri = request.raw().getParameter(URI_KEY);
            String sha1 = request.raw().getParameter(SHA1_KEY);

            // Validate parameters
            if (StringUtils.isBlank(result.uri)) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: uri is required but none provided");

                response.status(BAD_REQUEST_400);
                result.error = true;
                result.message = "Please provide uri and sha1 parameters.";
                return result;
            }

            log.uri(result.uri);

            if (StringUtils.isBlank(sha1)) {
                log.responseStatus(BAD_REQUEST_400)
                        .warn("bad request: verify: sha1 is required but none provided");
                response.status(BAD_REQUEST_400);

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

            response.status(INTERNAL_SERVER_ERROR_500);
            result.error = true;
            result.message = ExceptionUtils.getStackTrace(e);
        }
        return result;
    }
}
