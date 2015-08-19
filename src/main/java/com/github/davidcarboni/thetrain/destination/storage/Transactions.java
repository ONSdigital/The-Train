package com.github.davidcarboni.thetrain.destination.storage;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.davidcarboni.thetrain.destination.json.Transaction;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class for working with {@link Transaction} instances.
 */
public class Transactions {

    static final String JSON = "transaction.json";
    static final String CONTENT = "content";
    static final String BACKUP = "backup";

    static Map<String, Transaction> transactionMap = new ConcurrentHashMap<>();
    static Path transactionStore;

    public static Transaction create() throws IOException {

        Transaction transaction = new Transaction();

        // Generate the file structure
        Path path = path(transaction.id());
        Files.createDirectory(path);
        Path json = path.resolve(JSON);
        try (OutputStream output = Files.newOutputStream(json)) {
            Serialiser.serialise(output, transaction);
            Files.createDirectory(content(transaction));
            Files.createDirectory(backup(transaction));

            // Return the new transaction:
            transactionMap.put(transaction.id(), transaction);
            return transaction;
        }
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

        if (StringUtils.isNotBlank(id)) {
            synchronized (transactionMap) {
                if (!transactionMap.containsKey(id)) {
                    // Generate the file structure
                    Path transactionPath = path(id);
                    if (transactionPath != null && Files.exists(transactionPath)) {
                        final Path json = transactionPath.resolve(JSON);
                        try (InputStream input = Files.newInputStream(json)) {
                            Transaction transaction = Serialiser.deserialise(input, Transaction.class);
                            transactionMap.put(id, transaction);
                        }
                    }
                }
            }

            result = transactionMap.get(id);
        }

        return result;
    }

    /**
     * Reads the transaction Json specified by the given id.
     *
     * @param transaction The {@link Transaction}.
     * @return The {@link Transaction} if it exists, otherwise null.
     * @throws IOException If an error occurs in reading the transaction Json.
     */
    public static void update(Transaction transaction) throws IOException {

        if (transaction != null && transactionMap.containsKey(transaction.id())) {
            // The transaction passed in should always be an instance from the map
            // otherwise there's potential to lose updates.
            // NB the unit of synchronization is always a Transaction object.
            Transaction read = transactionMap.get(transaction.id());
            synchronized (read) {
                Path transactionPath = path(transaction.id());
                if (transactionPath != null && Files.exists(transactionPath)) {
                    final Path json = transactionPath.resolve(JSON);
                    try (OutputStream output = Files.newOutputStream(json)) {
                        Serialiser.serialise(output, read);
                    }
                }
            }
        }
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

    /**
     * Resolved the path under which content being published will be stored prior to being committed to the website content store.
     *
     * @param transaction The {@link Transaction} within which to determine the {@value #CONTENT} directory.
     * @return The {@link Path} of the {@value #CONTENT} directory for the specified transaction.
     * @throws IOException If an error occurs in determining the path.
     */
    public static Path backup(Transaction transaction) throws IOException {
        Path result = null;
        Path path = path(transaction);
        if (path != null) {
            result = path.resolve(BACKUP);
        }
        return result;
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
        Path path = path(transaction.id());
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
            // That's is why this is not synchronized:
            transactionStore = Files.createTempDirectory(Transactions.class.getSimpleName());
        }
        return transactionStore;
    }
}
