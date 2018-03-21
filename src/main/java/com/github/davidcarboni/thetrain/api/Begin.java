package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.api.common.Endpoint;
import com.github.davidcarboni.thetrain.json.Result;
import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.davidcarboni.thetrain.logging.Logger;
import com.github.davidcarboni.thetrain.storage.Transactions;
import org.apache.commons.fileupload.FileUploadException;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

import static com.github.davidcarboni.thetrain.logging.Logger.newLogger;

/**
 * Endpoint to start a new {@link Transaction}.
 */
@Api
public class Begin implements Endpoint {

    @POST
    public Result beginTransaction(HttpServletRequest request,
                                   HttpServletResponse response) throws IOException, FileUploadException {
        Logger logger = newLogger().endpoint(this);
        Transaction transaction = null;

        logger.info("creating publishing transaction");

        try {
            String encryptionPassword = request.getParameter(ENCRYPTION_PASSWORD_KEY);
            transaction = Transactions.create(encryptionPassword);
            logger.transactionID(transaction.id()).info("transaction created successfully");
            return new Result("New transaction created.", false, transaction);

        } catch (IOException e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            logger.error(e, "unexpected error while attempting to create transaction");
            return new Result("beingTransaction encountered unexpected error", true, transaction);
        }
    }
}
