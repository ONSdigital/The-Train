package com.github.davidcarboni.thetrain.destination.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.destination.json.Result;
import com.github.davidcarboni.thetrain.destination.json.Transaction;
import com.github.davidcarboni.thetrain.destination.storage.Transactions;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

/**
 * Created by david on 30/07/2015.
 */
@Api
public class begin {

    @POST
    public Result beginTransaction(HttpServletRequest request,
                               HttpServletResponse response) throws IOException, FileUploadException {

        Transaction transaction = null;
        String message;
        boolean error = false;

        try {
             transaction = Transactions.create();
            message = "New transaction created: " + transaction.id;
        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            error = true;
            message = ExceptionUtils.getStackTrace(e);
        }

        return new Result(message, error, transaction);
    }
}
