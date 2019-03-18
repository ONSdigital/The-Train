package com.github.onsdigital.thetrain.routes;

import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.helpers.FileUploadHelper;
import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.service.PublisherService;
import com.github.onsdigital.thetrain.service.TransactionsService;
import com.github.onsdigital.thetrain.storage.TransactionUpdate;
import org.apache.http.HttpStatus;
import spark.Request;
import spark.Response;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.zip.ZipInputStream;

import static com.github.onsdigital.thetrain.logging.TrainEvent.info;

public class AddFileToTransaction extends BaseHandler {

    static final String ADD_FILE_ERR_MSG = "error adding file to transaction";

    private TransactionsService transactionsService;
    private PublisherService publisherService;
    private FileUploadHelper fileUploadHelper;

    /**
     * Construct a new add file to transaction Route.
     */
    public AddFileToTransaction(TransactionsService transactionsService, PublisherService publisherService,
                                FileUploadHelper fileUploadHelper) {
        this.transactionsService = transactionsService;
        this.publisherService = publisherService;
        this.fileUploadHelper = fileUploadHelper;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        Date startDate = new Date();

        Transaction transaction = transactionsService.getTransaction(request);
        String uri = getURI(request);

        try {
            if (isZipped(request)) {
                handleZipRequest(request, transaction, uri);
            } else {
                handleNonZipRequest(request, transaction, uri, startDate);
            }
        } finally {
            transactionsService.tryUpdateAsync(transaction);
        }

        info().transactionID(transaction.id())
                .data("uri", uri)
                .log("file added to publish transaction successfully");

        return new Result("Published to " + uri, false, transaction);
    }

    /**
     * Handle a zip file request
     */
    private void handleZipRequest(Request request, Transaction transaction, String uri) throws PublishException {
        boolean isSuccess = false;
        info().transactionID(transaction.id()).data("uri", uri).log("attempting to add zip files to transactions");
        try (
                InputStream data = fileUploadHelper.getFileInputStream(request.raw(), transaction.id());
                ZipInputStream input = new ZipInputStream(new BufferedInputStream(data))
        ) {
            isSuccess = publisherService.addFiles(transaction, uri, input);
        } catch (Exception e) {
            throw new PublishException("error adding zipped files to transaction", e, transaction, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

        if (!isSuccess) {
            throw new PublishException("error adding zipped files to transaction", transaction, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
        info().log("succcessfully added zip content to transaction");
    }

    /**
     * handle a single file request.
     */
    private void handleNonZipRequest(Request request, Transaction transaction, String uri, Date startDate)
            throws BadRequestException, PublishException {
        boolean isSuccess = false;
        info().transactionID(transaction.id()).data("uri", uri).log("attempting to add file to transactions");
        try (
                InputStream data = fileUploadHelper.getFileInputStream(request.raw(), transaction.id());
                InputStream bis = new BufferedInputStream(data)
        ) {
            TransactionUpdate update = publisherService.addContentToTransaction(transaction, uri, bis, startDate);
            isSuccess = update.isSuccess();
            transaction.addUri(update.getUriInfo());

        } catch (BadRequestException e) {
            // re-throw
            throw e;
        } catch (Exception e) {
            // treat all others are a publish exception.
            throw new PublishException(ADD_FILE_ERR_MSG, e, transaction, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

        if (!isSuccess) {
            throw new PublishException(ADD_FILE_ERR_MSG, transaction, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

        info().transactionID(transaction.id()).data("uri", uri).log("file successfully added to transaction");
    }
}
