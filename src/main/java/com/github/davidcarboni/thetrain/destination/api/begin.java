package com.github.davidcarboni.thetrain.destination.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.destination.json.Transaction;
import com.github.davidcarboni.thetrain.destination.storage.Transactions;
import org.apache.commons.fileupload.FileUploadException;

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
    public Transaction beginTransaction(HttpServletRequest request,
                               HttpServletResponse response) throws IOException, FileUploadException {
        return Transactions.create();
    }
}
