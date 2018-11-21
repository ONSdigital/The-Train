package com.github.onsdigital.thetrain.service;

import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.json.request.Manifest;
import com.github.onsdigital.thetrain.storage.TransactionUpdate;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Date;
import java.util.zip.ZipInputStream;

public interface PublisherService {

    /**
     * @return get the website path configuration.
     * @throws PublishException error getting the config value.
     */
    Path websitePath() throws PublishException;

    /**
     * Commit the transaction.
     *
     * @param transaction the transaction to commit.
     * @param website     the website path value.
     * @return true if successful false if failed
     * @throws PublishException error while attempting to commit the transaction.
     */
    boolean commit(Transaction transaction, Path website) throws PublishException;

    /**
     * Attempt to rollback a transaction.
     *
     * @param transaction the transaction to rollback.
     * @return true if successful, false otherwise.
     * @throws PublishException error attempting to rollback the transaction.
     */
    boolean rollback(Transaction transaction) throws PublishException;

    /**
     * Copy the files from the publishing manifest into the publishing transaction.
     *
     * @param transaction the target transaction.
     * @param manifest    the manifest you wish to use.
     * @param websitePath the website path config.
     * @return the number of files copied.
     * @throws PublishException error while attempting to copy the files into the transaction.
     */
    int copyFilesIntoTransaction(Transaction transaction, Manifest manifest, Path websitePath) throws PublishException;

    /**
     * Add the files to be deleted to the transaction.
     *
     * @param transaction the target transaction.
     * @param manifest    the manifest the use.
     * @return the number of delete markers added to the transaction.
     * @throws PublishException error while attempting to add the delete markers.
     */
    int addFilesToDelete(Transaction transaction, Manifest manifest) throws PublishException;

    /**
     * Add a single content item to the publishing transaction.
     *
     * @param transaction the target transaction.
     * @param uri         the uri of the content being added.
     * @param input       an inputstream of the content data.
     * @param startDate   the date time to use.
     * @return an {@link TransactionUpdate} containing the outcome of the operation.
     * @throws PublishException error while attempting to add the content to the transaction.
     */
    TransactionUpdate addContentToTransaction(Transaction transaction, String uri, InputStream input, Date startDate)
            throws PublishException;

    /**
     * Add the content of a zip file to the publishing transaction.
     *
     * @param transaction the target transaction.
     * @param uri         the uri the content.
     * @param zip         a {@link ZipInputStream} for the zipped content to add.
     * @return true if successful, false otherwise.
     * @throws PublishException error while attempting add the content of the zip.
     */
    boolean addFiles(final Transaction transaction, String uri, final ZipInputStream zip) throws PublishException;
}
