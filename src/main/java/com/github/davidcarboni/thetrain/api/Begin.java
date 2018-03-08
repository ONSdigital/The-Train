package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.json.Result;
import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.davidcarboni.thetrain.storage.Transactions;
import org.apache.commons.fileupload.FileUploadException;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

import static com.github.davidcarboni.thetrain.logging.LogBuilder.error;
import static com.github.davidcarboni.thetrain.logging.LogBuilder.info;

/**
 * API to start a new {@link Transaction}.
 */
@Api
public class Begin {

    private static final String ENCRYPTION_PWORD_KEY = "encryptionPassword";

    @POST
    public Result beginTransaction(HttpServletRequest request,
                                   HttpServletResponse response) throws IOException, FileUploadException {
        Transaction transaction = null;
        info("begin: beginning publishing transaction").log();

        try {
            String encryptionPassword = request.getParameter(ENCRYPTION_PWORD_KEY);
            transaction = Transactions.create(encryptionPassword);
            info("begin: new publishing transaction created successfully")
                    .transactionID(transaction.id())
                    .log();
            return new Result("New transaction created.", false, transaction);

        } catch (IOException e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            error(e, "begin: unexpected error while attempting to create transaction").log();
            return new Result("beingTransaction encountered unexpected error", true, transaction);
        }
    }
}
