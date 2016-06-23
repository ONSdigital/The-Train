package com.github.davidcarboni.thetrain.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.json.Result;
import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.davidcarboni.thetrain.logging.Log;
import com.github.davidcarboni.thetrain.storage.Transactions;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

/**
 * API to start a new {@link Transaction}.
 */
@Api
public class Begin {

    @POST
    public Result beginTransaction(HttpServletRequest request,
                                   HttpServletResponse response) throws IOException, FileUploadException {

        Transaction transaction = null;
        String message;
        boolean error = false;

        try {

            String encryptionPassword = request.getParameter("encryptionPassword");
            transaction = Transactions.create(encryptionPassword);
            message = "New transaction created.";

        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            error = true;
            message = ExceptionUtils.getStackTrace(e);
        }

        Log.info(message + (transaction != null ? " (transaction " + transaction.id() + ")" : ""));
        return new Result(message, error, transaction);
    }
}
