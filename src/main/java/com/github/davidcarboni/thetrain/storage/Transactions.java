package com.github.davidcarboni.thetrain.storage;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davidcarboni.thetrain.helpers.Configuration;
import com.github.davidcarboni.thetrain.helpers.PathUtils;
import com.github.davidcarboni.thetrain.json.Transaction;
import static com.github.davidcarboni.thetrain.logging.LogBuilder.error;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.github.davidcarboni.thetrain.logging.LogBuilder.info;
import static com.github.davidcarboni.thetrain.logging.LogBuilder.warn;

/**
 * Class for working with {@link Transaction} instances.
 */
public class Transactions {

    static final String JSON = "transaction.json";
    static final String CONTENT = "content";
    static final String BACKUP = "backup";

    static final Map<String, Transaction> transactionMap = new ConcurrentHashMap<>();
    static final Map<String, ExecutorService> transactionExecutorMap = new ConcurrentHashMap<>();

    static Path transactionStore;


    /**
     * Creates a new transaction.
     *
     * @param encryptionPassword If this is not blank, encryption will be enabled for the transaction.
     * @return The details of the newly created transaction.
     * @throws IOException If a filesystem error occurs in creating the transaction.
     */
    public static Transaction create(String encryptionPassword) throws IOException {

        Transaction transaction = new Transaction();

        // Enable encryption if requested
        transaction.enableEncryption(encryptionPassword);

        // Generate the file structure
        Path path = path(transaction.id());
        Files.createDirectory(path);
        Path json = path.resolve(JSON);
        try (OutputStream output = Files.newOutputStream(json)) {
            objectMapper().writeValue(output, transaction);
            Files.createDirectory(path.resolve(CONTENT));
            Files.createDirectory(path.resolve(BACKUP));

            transactionMap.put(transaction.id(), transaction);

            if (!transactionExecutorMap.containsKey(transaction.id())) {
                transactionExecutorMap.put(transaction.id(), Executors.newSingleThreadExecutor());
            }

            return transaction;
        }
    }

    /**
     * Cleanup any resources held by the transaction.
     *
     * @param transaction
     */
    public static void end(Transaction transaction) {
        if (transactionExecutorMap.containsKey(transaction.id())) {
            transactionExecutorMap.get(transaction.id()).shutdown();
            transactionExecutorMap.remove(transaction.id());
        }

        if (transactionMap.containsKey(transaction.id())) {
            transactionMap.remove(transaction.id());
        }
    }

    /**
     * Reads the transaction Json specified by the given id.
     *
     * @param id The {@link Transaction} ID.
     * @return The {@link Transaction} if it exists, otherwise null.
     * @throws IOException If an error occurs in reading the transaction Json.
     */

    public static Transaction get(String id, String encryptionPassword) throws IOException {
        Transaction result = null;

        if (StringUtils.isNotBlank(id)) {

            if (!transactionMap.containsKey(id)) {
                // Generate the file structure
                Path transactionPath = path(id);
                if (transactionPath != null && Files.exists(transactionPath)) {
                    final Path json = transactionPath.resolve(JSON);
                    try (InputStream input = Files.newInputStream(json)) {
                        result = objectMapper().readValue(input, Transaction.class);
                    }
                }
            } else {
                result = transactionMap.get(id);

                if (result != null) {
                    result.enableEncryption(encryptionPassword);
                }
            }
        }

        return result;
    }

    public static void listFiles(Transaction transaction) throws IOException {
        Map<String, List<String>> list = new HashMap<>();

        Path content = content(transaction);
        if (Files.isDirectory(content)) {
            list.put("content", PathUtils.listUris(content));
        }
        Path backup = backup(transaction);
        if (Files.isDirectory(backup)) {
            list.put("backup", PathUtils.listUris(backup));
        }

        transaction.files = list;
    }

    /**
     * Queue a task to update the transaction file. This task will only get run if the transaction
     * is not already committed.
     *
     * @param transactionId
     * @return
     * @throws IOException
     */
    public static Future<Boolean> tryUpdateAsync(final String transactionId) throws IOException {
        Future<Boolean> future = null;

        if (transactionExecutorMap.containsKey(transactionId)) {
            ExecutorService transactionUpdateExecutor = transactionExecutorMap.get(transactionId);

            future = transactionUpdateExecutor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    Boolean result = false;

                    try {
                        if (transactionExecutorMap.containsKey(transactionId)
                                && transactionMap.containsKey(transactionId)) {
                            // The transaction passed in should always be an instance from the map
                            // otherwise there's potential to lose updates.
                            // NB the unit of synchronization is always a Transaction object.
                            Transaction read = transactionMap.get(transactionId);
                            synchronized (read) {
                                Path transactionPath = path(transactionId);
                                if (transactionPath != null && Files.exists(transactionPath)) {
                                    final Path json = transactionPath.resolve(JSON);

                                    try (OutputStream output = Files.newOutputStream(json)) {
                                        objectMapper().writeValue(output, read);
                                    }
                                }
                                result = true;
                            }
                        } else {
                            // do nothing
                        }
                    } catch (IOException exception) {
                        error(exception, "").log();
                    }

                    return result;
                }
            });
        }

        return future;
    }

    /**
     * Reads the transaction Json specified by the given id.
     *
     * @param transaction The {@link Transaction}.
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
                    info("writing transaction file")
                            .addParameter("path", json.toString())
                            .log();
                    try (OutputStream output = Files.newOutputStream(json)) {
                        objectMapper().writeValue(output, read);
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
        Path path = path(transaction.id());
        if (path != null) {
            path = path.resolve(CONTENT);
            if (Files.exists(path)) {
                result = path;
            } else {
                warn("content path does not exist")
                        .transactionID(transaction.id())
                        .addParameter("path", path.toString())
                        .log();
            }
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
        Path path = path(transaction.id());
        if (path != null) {
            path = path.resolve(BACKUP);
            if (Files.exists(path)) result = path;
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
     * For development purposes, if no {@value Configuration#TRANSACTION_STORE} configuration value is set
     * then a temponary folder is created.
     *
     * @return The {@link Path} to the storage directory for all transactions.
     * @throws IOException If an error occurs.
     */
    static Path store() throws IOException {
        if (transactionStore == null) {

            // Production configuration
            String transactionStorePath = Configuration.transactionStore();
            if (StringUtils.isNotBlank(transactionStorePath)) {
                Path path = Paths.get(transactionStorePath);
                if (Files.isDirectory(path)) {
                    transactionStore = path;
                    info("TRANSACTION_STORE configured")
                            .addParameter("path", path.toString())
                            .log();
                } else {
                    info("transaction store directory invalid")
                            .addParameter("path", path)
                            .log();
                }
            }

            // Development fallback
            if (transactionStore == null) {
                transactionStore = Files.createTempDirectory(Transactions.class.getSimpleName());
                info("temporary transaction store created")
                        .addParameter("path", transactionStore.toString())
                        .log();
                info("please configure a TRANSACTION_STORE variable to configure this directory in production.").log();
            }

        }
        return transactionStore;
    }

    private static ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return objectMapper;
    }
}
