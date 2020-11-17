package com.github.onsdigital.thetrain.helpers.uploads;

import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.json.Transaction;
import spark.Request;

@FunctionalInterface
public interface CloseablePartSupplier {

    CloseablePart getFilePart(Request req, Transaction transaction) throws PublishException, BadRequestException;
}
