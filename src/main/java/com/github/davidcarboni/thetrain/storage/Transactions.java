package com.github.davidcarboni.thetrain.storage;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davidcarboni.thetrain.helpers.Configuration;
import com.github.davidcarboni.thetrain.helpers.PathUtils;
import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.davidcarboni.thetrain.logging.LogBuilder;
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

import static com.github.davidcarboni.thetrain.logging.LogBuilder.logBuilder;

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
     * @return The details of the newly created transaction.
     * @throws IOException If a filesystem error occurs in creating the transaction.
     */
    public static Transaction create() throws IOException {

        Transaction transaction = new Transaction();
        LogBuilder log = logBuilder().transactionID(transaction.id());

        // Generate the file structure
        Path path = path(transaction.id());
        Files.createDirectory(path);
        Path json = path.resolve(JSON);
        try (OutputStream output = Files.newOutputStream(json)) {
            objectMapper().writeValue(output, transaction);
            Files.createDirectory(path.resolve(CONTENT));
            Files.createDirectory(path.resolve(BACKUP));
            log.info("transaction written to disk successfully");

            transactionMap.put(transaction.id(), transaction);

            log.info("transaction added to in-memory storage");

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

    public static Transaction get(String id) throws IOException {
        LogBuilder log = logBuilder().transactionID(id);
        Transaction result = null;

        try {
            if (StringUtils.isNotBlank(id)) {

                if (!transactionMap.containsKey(id)) {
                    log.info("transaction does not exist in in-memory storage, attempting to read from file system");
                    // Generate the file structure
                    Path transactionPath = path(id);
                    if (transactionPath != null && Files.exists(transactionPath)) {
                        final Path json = transactionPath.resolve(JSON);
                        try (InputStream input = Files.newInputStream(json)) {
                            result = objectMapper().readValue(input, Transaction.class);
                        }
                    }
                } else {
                    log.info("retrieving transaction from in-memory storage");
                    result = transactionMap.get(id);
                }
            }
            return result;
        } catch (Exception e) {
            log.transactionID(id).error(e, "get transaction returned an unexpected error");
            throw e;
        }
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
                        logBuilder().error(exception, "tryUpdateAsync: unexpected error encountered");
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
        LogBuilder log = logBuilder();
        if (transaction != null && transactionMap.containsKey(transaction.id())) {
            // The transaction passed in should always be an instance from the map
            // otherwise there's potential to lose updates.
            // NB the unit of synchronization is always a Transaction object.
            Transaction read = transactionMap.get(transaction.id());
            synchronized (read) {
                Path transactionPath = path(transaction.id());
                if (transactionPath != null && Files.exists(transactionPath)) {
                    final Path json = transactionPath.resolve(JSON);
                    log.addParameter("path", json.toString())
                            .info("writing transaction file");

                    try (OutputStream output = Files.newOutputStream(json)) {
                        objectMapper().writeValue(output, read);
                        log.info("writing transaction file completed successfully");
                    } catch (Exception e) {
                        log.addParameter("path", json.toString())
                                .error(e, "error while writing transaction to file");
                        throw e;
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
                logBuilder()
                        .transactionID(transaction.id())
                        .addParameter("path", path.toString())
                        .warn("content path does not exist");
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
        LogBuilder logBuilder = logBuilder();

        if (transactionStore == null) {

            // Production configuration
            String transactionStorePath = Configuration.transactionStore();
            if (StringUtils.isNotBlank(transactionStorePath)) {
                Path path = Paths.get(transactionStorePath);
                if (Files.isDirectory(path)) {
                    transactionStore = path;

                    logBuilder.addParameter("path", path.toString())
                            .info("TRANSACTION_STORE configured");
                } else {

                    logBuilder.addParameter("path", path)
                            .info("transaction store directory invalid");
                }
            }

            // Development fallback
            if (transactionStore == null) {
                transactionStore = Files.createTempDirectory(Transactions.class.getSimpleName());
                logBuilder.addParameter("path", transactionStore.toString()).info("temporary transaction store " +
                        "created");
                logBuilder.info("please configure a TRANSACTION_STORE variable to configure this directory in production.");
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
