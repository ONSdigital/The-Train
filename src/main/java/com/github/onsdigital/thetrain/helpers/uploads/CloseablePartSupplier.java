package com.github.onsdigital.thetrain.helpers.uploads;

import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.json.Transaction;
import spark.Request;

/**
 * Defines an object for obtaining a file upload {@link CloseablePart} from a {@link Request}.
 */
@FunctionalInterface
public interface CloseablePartSupplier {

    /**
     * Get a file {@link CloseablePart} from a file upload request.
     *
     * @param req         the {@link Request} to get the {@link CloseablePart} from. Required and cannot be null.
     * @param transaction the publishing {@link Transaction} the file upload is being added too. Required and cannot
     *                    be null.
     * @return a {@link CloseablePart} for the uploaded file.
     * @throws PublishException    unexpected error getting the {@link CloseablePart} from the request.
     * @throws BadRequestException thrown if either request or transaction are null, the file upload part is
     *                             null or invalid.
     */
    CloseablePart getFilePart(Request req, Transaction transaction) throws PublishException, BadRequestException;
}