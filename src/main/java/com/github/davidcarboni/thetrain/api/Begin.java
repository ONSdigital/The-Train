package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.api.common.Endpoint;
import com.github.davidcarboni.thetrain.json.Result;
import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.davidcarboni.thetrain.logging.Logger;
import com.github.davidcarboni.thetrain.service.TransactionsService;
import com.github.davidcarboni.thetrain.service.TransactionsServiceImpl;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

import static com.github.davidcarboni.thetrain.logging.Logger.newLogger;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Endpoint to start a new {@link Transaction}.
 */
@Api
public class Begin implements Endpoint {

    private TransactionsService transactionsService;

    @POST
    public Result beginTransaction(HttpServletRequest request,
                                   HttpServletResponse response) throws IOException, FileUploadException {
        Logger logger = newLogger().endpoint(this);
        Transaction transaction = null;

        try {
            String encryptionPassword = request.getParameter(ENCRYPTION_PASSWORD_KEY);
            if (StringUtils.isEmpty(encryptionPassword)) {
                logger.responseStatus(BAD_REQUEST_400)
                        .warn("encryption password required but none provided");
                response.setStatus(BAD_REQUEST_400);
                return new Result("encryption password required but none provided", true, null);
            }

            logger.info("creating new publishing transaction");
            transaction = getTransactionService().create(encryptionPassword);
            logger.transactionID(transaction.id()).info("transaction created successfully");

            response.setStatus(OK_200);
            return new Result("New transaction created.", false, transaction);

        } catch (IOException e) {
            response.setStatus(INTERNAL_SERVER_ERROR_500);
            logger.error(e, "unexpected error while attempting to create transaction");
            return new Result("beingTransaction encountered unexpected error", true, transaction);
        }
    }

    private TransactionsService getTransactionService() {
        if (transactionsService == null) {
            this.transactionsService = new TransactionsServiceImpl();
        }
        return transactionsService;
    }
}
