package com.github.davidcarboni.thetrain.destination.storage;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.davidcarboni.thetrain.destination.json.DateConverter;
import com.github.davidcarboni.thetrain.destination.json.Timing;
import com.github.davidcarboni.thetrain.destination.json.Transaction;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.concurrent.*;

/**
 * Created by david on 03/08/2015.
 */
public class Transactions {

    static final String JSON = "transaction.json";
    static final String CONTENT = "content";

    static ExecutorService transactionUpdates = Executors.newSingleThreadExecutor();
    static Path transactionStore;
    static Path contentStore;

    public static Transaction create() throws IOException {

        // Whilst an ID collision is technicall possible it's a
        // theoretical rather than a practical consideration.
        Transaction transaction = new Transaction();
        transaction.id = Random.id();
        transaction.startDate = DateConverter.toString(new Date());

        // Generate the file structure
        Path path = path(transaction.id);
        Files.createDirectory(path);
        Path json = path.resolve(JSON);
        try (OutputStream output = Files.newOutputStream(json)) {
            Serialiser.serialise(output, transaction);
        }
        Files.createDirectory(content(transaction));

        // Return the new transaction:
        return transaction;
    }

    /**
     * Reads the transaction Json specified by the given id.
     *
     * @param id The {@link Transaction} ID.
     * @return The {@link Transaction} if it exists, otherwise null.
     * @throws IOException If an error occurs in reading the transaction Json.
     */
    public static Transaction get(String id) throws IOException {
        Transaction result = null;

        // Generate the file structure
        Path transactionPath = path(id);
        if (transactionPath != null && Files.exists(transactionPath)) {
            //try {
                final Path json = transactionPath.resolve(JSON);
                //Future<Transaction> future = transactionUpdates.submit(new Callable<Transaction>() {
                //    @Override
                //    public Transaction call() throws IOException {
                        try (InputStream input = Files.newInputStream(json)) {
                            return Serialiser.deserialise(input, Transaction.class);
                        }
                //    }
                //});
                //result = future.get();
            //} catch (InterruptedException | ExecutionException e) {
            //    throw new IOException("Error reading transaction Json", e);
            //}
        }

        return result;
    }

    /**
     * Resolved the path under which content being published will be stored prior to being committed to the website content store.
     *
     * @param transaction The {@link Transaction} within which to determine the {@value #CONTENT} directory.
     * @return The {@link Path} of the {@value #CONTENT} directory for the specified transaction.
     * @throws IOException If an error occurs in determining the path.
     */
    public static Path content(Transaction transaction) throws IOException {
        Path result = null;
        Path path = path(transaction);
        if (path != null) {
            result = path.resolve(CONTENT);
        }
        return result;
    }

    public static void addFile(final Transaction transaction, final Timing timing) {
        // We use a single-threaded pool to read the transaction Json and write
        // updates asynchronously in series so we can manage concurrent updates.
        transactionUpdates.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Transaction read = Transactions.get(transaction.id);
                    read.uris.add(timing);
                    Path path = path(transaction);
                    try (OutputStream output = Files.newOutputStream(path)) {
                        Serialiser.serialise(output, read);
                    }
                } catch (IOException e) {
                    // If we fail now, we probably have bigger issues:
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Determines the directory for a {@link Transaction}.
     *
     * @param transaction The {@link Transaction}
     * @return The {@link Path} for the specified transaction, or null if the ID is blank or the path does not exist.
     * @throws IOException If an error occurs in determining the path.
     */
    static Path path(Transaction transaction) throws IOException {
        if (transaction == null) {
            throw new IllegalArgumentException("Please provide a valid transaction.");
        }
        Path result = null;
        Path path = path(transaction.id);
        if (Files.exists(path)) {
            result = path;
        }
        return result;
    }

    /**
     * Determines the directory for a {@link Transaction}.
     *
     * @param id The {@link Transaction} ID.
     * @return The {@link Path} for the specified transaction, or null if the ID is blank.
     * @throws IOException If an error occurs in determining the path.
     */
    static Path path(String id) throws IOException {
        Path result = null;
        if (StringUtils.isNotBlank(id)) {
            result = store().resolve(id);
        }
        return result;
    }

    /**
     * @return The {@link Path} to the storage directory for all transactions.
     * @throws IOException If an error occurs.
     */
    static Path store() throws IOException {
        if (transactionStore == null) {
            // In production, the transaction store will be configured,
            // so this should only be needed in development and testing.
            // That's is why it's not synchronized.
            transactionStore = Files.createTempDirectory(Transactions.class.getSimpleName());
        }
        return transactionStore;
    }
}
