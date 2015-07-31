package com.github.davidcarboni.thetrain.destination.api;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.thetrain.destination.json.DateConverter;
import com.github.davidcarboni.thetrain.destination.json.Transaction;
import org.apache.commons.fileupload.FileUploadException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;
import java.util.Date;

/**
 * Created by david on 30/07/2015.
 */
@Api
public class begin {

    @POST
    public Transaction begin(HttpServletRequest request,
                               HttpServletResponse response) throws IOException, FileUploadException {

        Transaction transaction = new Transaction();
        transaction.id = Random.id();
        transaction.startDate = DateConverter.toString(new Date());
        return transaction;
    }
}
