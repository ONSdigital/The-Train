package com.github.onsdigital.thetrain.service;

import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.json.request.Manifest;
import com.github.onsdigital.thetrain.storage.Publisher;
import com.github.onsdigital.thetrain.storage.TransactionUpdate;
import com.github.onsdigital.thetrain.storage.Website;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Date;
import java.util.zip.ZipInputStream;

public class PublisherServiceImpl implements PublisherService {

    static final String COPY_FILE_TO_TRANS_ERR = "error copying files to transaction";
    static final String ADD_FILES_TO_TRANS_ERR = "error adding files to transaction";
    static final String ADD_FILES_FROM_ZIP_TO_TRANS_ERR = "error adding files from zip to transaction";
    static final String ADD_DELETES_TO_TRANS_ERR = "error adding files to delete to transaction";
    static final String WEDBSITE_PATH_NULL_ERR = "error getting website path config, expected value but was null";
    static final String WEDBSITE_PATH_ERR = "error getting website path config";
    static final String COMMIT_TRANS_ERROR = "error committing publishing transaction";
    static final String ROLLBACK_TRANS_ERROR = "error rolling back publishing transaction";

    private Publisher publisher;

    public PublisherServiceImpl() {
        this.publisher = Publisher.getInstance();
    }

    public PublisherServiceImpl(Publisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public Path websitePath() throws PublishException {
        try {
            Path website = Website.path();
            if (website == null) {
                throw new PublishException(WEDBSITE_PATH_NULL_ERR);
            }
            return website;
        } catch (IOException e) {
            throw new PublishException(WEDBSITE_PATH_ERR, e);
        }
    }

    @Override
    public boolean commit(Transaction transaction, Path website) throws PublishException {
        try {
            return publisher.commit(transaction, website);
        } catch (Exception e) {
            throw new PublishException(COMMIT_TRANS_ERROR, e, transaction, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public boolean rollback(Transaction transaction) throws PublishException {
        try {
            return publisher.rollback(transaction);
        } catch (Exception e) {
            throw new PublishException(ROLLBACK_TRANS_ERROR, e);
        }
    }

    @Override
    public int copyFilesIntoTransaction(Transaction transaction, Manifest manifest, Path websitePath) throws PublishException {
        try {
            return publisher.copyFilesIntoTransaction(transaction, manifest, websitePath);
        } catch (Exception e) {
            throw new PublishException(COPY_FILE_TO_TRANS_ERR, e, transaction, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public int addFilesToDelete(Transaction transaction, Manifest manifest) throws PublishException {
        try {
            return publisher.addFilesToDelete(transaction, manifest);
        } catch (Exception e) {
            throw new PublishException(ADD_DELETES_TO_TRANS_ERR, e, transaction, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public TransactionUpdate addContentToTransaction(Transaction transaction, String uri, InputStream input,
                                                     Date startDate) throws PublishException {
        try {
            return publisher.addContentToTransaction(transaction, uri, input, startDate);
        } catch (Exception e) {
            throw new PublishException(ADD_FILES_TO_TRANS_ERR, e, transaction, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public boolean addFiles(Transaction transaction, String uri, ZipInputStream zip) throws PublishException {
        try {
            return publisher.addFiles(transaction, uri, zip);
        } catch (Exception e) {
            throw new PublishException(ADD_FILES_FROM_ZIP_TO_TRANS_ERR, e, transaction,
                    HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
