package com.github.onsdigital.thetrain.storage;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.onsdigital.thetrain.configuration.AppConfiguration;
import com.github.onsdigital.thetrain.configuration.ConfigurationException;
import com.github.onsdigital.thetrain.configuration.ConfigurationUtils;
import com.github.onsdigital.thetrain.helpers.PathUtils;
import com.github.onsdigital.thetrain.helpers.Slack;
import com.github.onsdigital.thetrain.json.Transaction;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

import static com.github.onsdigital.thetrain.logging.TrainEvent.error;
import static com.github.onsdigital.thetrain.logging.TrainEvent.info;

// TODO FIX ME - Make this class a singleton with non static methods.

/**
 * Class for working with {@link Transaction} instances.
 */
public class Transactions {

    static final String JSON = "transaction.json";
    static final String CONTENT = "content";
    static final String BACKUP = "backup";

    private static Path transactionStore;
    private static Path archivedTransactionStore;
    private static ObjectMapper objectMapper;
    private static Map<String, Transaction> transactionMap;
    private static Map<String, ExecutorService> transactionExecutorMap;
    private static HashMap<String, Integer> reasonsForArchiving;

    public static void init(Path transactionStorePath, Path transactionArchiveStorePath) {
        transactionStore = transactionStorePath;
        archivedTransactionStore = transactionArchiveStorePath;
        objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        transactionMap = new ConcurrentHashMap<>();
        transactionExecutorMap = new ConcurrentHashMap<>();
        reasonsForArchiving = new HashMap<>();

        info().log("transaction and transaction archive store initialisation completed");

        long numberArchived = archiveTransactions();
        info().log("archived " + Long.toString(numberArchived) + " transactions");
    }

    public static Map<String, Transaction> getTransactionMap() {
        return transactionMap;
    }

    public static Map<String, ExecutorService> getTransactionExecutorMap() {
        return transactionExecutorMap;
    }

    /**
     * Creates a new transaction.
     *
     * @return The details of the newly created transaction.
     * @throws IOException If a filesystem error occurs in creating the transaction.
     */
    public static Transaction create() throws IOException {
        Transaction transaction = new Transaction();

        // Generate the file structure
        Path path = path(transaction.id());
        Files.createDirectory(path);
        Path json = path.resolve(JSON);
        try (OutputStream output = Files.newOutputStream(json)) {
            objectMapper.writeValue(output, transaction);
            Files.createDirectory(path.resolve(CONTENT));
            Files.createDirectory(path.resolve(BACKUP));
            info().transactionID(transaction.id())
                    .log("transaction written to disk successfully");

            transactionMap.put(transaction.id(), transaction);

            info().transactionID(transaction.id())
                    .log("transaction added to in-memory storage");

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
     * Moves the transaction to the Archived folder
     *
     * @param id The {@link Transaction} ID.
     * @throws IOException If an error occurs in reading the transaction Json.
     */
    private static void moveTransactionToArchive(String id) throws IOException {
        String source = Paths.get(transactionStore.toString(), id).toAbsolutePath().toString();
        String dest = archivedTransactionStore.toAbsolutePath().toString();
        FileUtils.moveToDirectory(new File(source), new File(dest), true);
        // ToDo - Catch org.apache.commons.io.FileExistsException and rename the directory at the destination end wth additional random characters, to cover collisions, not necessarily caused by the random id, but more with restarts.
    }

    public static ArrayList<String> findTransactionsInFolder(String folder) throws IOException {
        File directory = new File(folder);
        if (!directory.exists() && !directory.isHidden() ) {
            throw new IOException("The transaction folder wasn't a directory:"+folder);
        }
        ArrayList<String> listIDs = new ArrayList<>();
        File[] visibleFiles = directory.listFiles((FileFilter) HiddenFileFilter.VISIBLE);
        // Loop round all files which are visible, then if its a Directory then record the directory name/ID
        for (File visibleFile : visibleFiles) {
            // ToDo - check the filename with a file mask.
            if(visibleFile.isDirectory()) listIDs.add(visibleFile.getName());
        }
        return listIDs;
    }


    private static void tallyArchivalReason(String reason) {
        Integer tally = reasonsForArchiving.get(reason);
        tally = (tally==null)?0:tally;
        tally ++;
        reasonsForArchiving.put(reason, tally);
    }

    public static long archiveTransactions() {
        // Get all the Transaction files
        ArrayList<String> transactionsInFolder = null;
        try {
            transactionsInFolder = findTransactionsInFolder(transactionStore.toAbsolutePath().toString());
        } catch (IOException e) {
            error().exception(e).log("IOException when findingTransactionsInFolder:"+transactionStore.toAbsolutePath().toString());
        }
        // Calculate the transaction threshold date and time by
        //  adding today's date and time with the duration in the environmental variable
        int minutes = 0;
        try {
            minutes = (int) // Parsing to int is not ideal, however MAXINT is equivalent to around 68 years.
                (ConfigurationUtils.getDurationEnvVar(AppConfiguration.ARCHIVING_TRANSACTIONS_THRESHOLD_ENV_VAR).getSeconds())/60;
        } catch (ConfigurationException e) {
            error().exception(e).log("ConfigurationException when looking for the Environment Variable:"+ AppConfiguration.ARCHIVING_TRANSACTIONS_THRESHOLD_ENV_VAR);
        }
        Date transactionThreshold = DateUtils.addMinutes(new Date(), -minutes);
        // Stores the ID of the transactions to be archived, and their associated Error's
        HashMap<String, Integer> transactionsToBeArchived = new HashMap<>();
        // ToDo ^ - Might be useful to use a threadsafe queue here and have another process moving the transactions
        //  in parallel to this process.  Perhaps using Collections.synchronizedMap or ConcurrentHashMap.
        // Loop round all transactions and check if startDate or endDate is older than
        // the transaction threshold date/time.
        for(String id: transactionsInFolder) {
            Transaction transaction = null;
            try {
                transaction = Transactions.get(id);
            } catch (IOException e) {
                error().transactionID(id).exception(e).log("IOException when getting the transaction:"+id);
                // Erroneous transactions also require archiving
                id = (id.equals(null))?"null":id;
                Integer size = (transaction==null)?0:transaction.errors().size();
                // ToDo - create another folder for ErroneousTransactions, and associated Env Variable
                transactionsToBeArchived.put(id,size);
                continue;
            }
            //
            // FIRSTLY CHECK if the status of the Transaction would indicate that it should be archived
            //
            if (transaction.getStatus() != null &&
                    (
                    Transaction.COMMITTED.equals(transaction.getStatus())
                    || Transaction.COMMIT_FAILED.equals(transaction.getStatus())
                    || Transaction.ROLLBACK_FAILED.equals(transaction.getStatus())
                    || Transaction.ROLLED_BACK.equals(transaction.getStatus())
                    )
            )
            {
                // This transaction should be archived
                transactionsToBeArchived.put(id,transaction.errors().size());
                // Increment counter reason for archiving transaction
                tallyArchivalReason(transaction.getStatus());
                // Move onto next transaction
                continue;
            }
            //
            // SECONDLY CHECK if the start or end dates indicate that it should be archived.
            //
            Date s = transaction.getStartDateObject();
            Date e = transaction.getEndDateObject();
            if (
                    s == null || // Archive erroneous transaction as there should always be a start date
                    s.before(transactionThreshold) ||  // Start Date threshold so archive
                    (e!=null && e.before(transactionThreshold))  // End Date after threshold so archive
            ) {
                // Transaction is older than allowable so store id, tally archival reason and move onto next transaction
                transactionsToBeArchived.put(id, transaction.errors().size());
                tallyArchivalReason("past threshold ("+transactionThreshold.toString()+")");
                continue;
                // ToDo - Extract details of Transaction for displaying in Slack Message
            }
            //
            // ToDo - THIRD CHECK - The datestamp of the newest Transaction folder/file
            //
        }
        //
        // FINALLY - Move the transactions to Archive folder and Send a Slack Message
        //
        for(Map.Entry<String, Integer> entry: transactionsToBeArchived.entrySet()) {
            String i = entry.getKey();
            try {
                moveTransactionToArchive(i);
            } catch (IOException e) {
                error().transactionID(i).exception(e).log("IOException when moving the transaction to archive, where id="+i);
            }
        }
        try {
            Slack.sendSlackArchivalReasons(reasonsForArchiving, "The-Train Initialisation Archival Complete ("+transactionsToBeArchived.size()+" archives)");
        } catch (Exception e) {
            error().log("error when sending Slack summary of The-Train archival summary."+e.getMessage());
        }
        return transactionsToBeArchived.size();
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

        try {
            if (StringUtils.isNotBlank(id)) {

                if (!transactionMap.containsKey(id)) {
                    info().transactionID(id)
                            .log("transaction does not exist in in-memory storage, attempting to read from file system");
                    // Generate the file structure
                    Path transactionPath = path(id);
                    if (transactionPath != null && Files.exists(transactionPath)) {
                        final Path json = transactionPath.resolve(JSON);
                        try (InputStream input = Files.newInputStream(json)) {
                            result = objectMapper.readValue(input, Transaction.class);
                        }
                    }
                } else {
                    info().transactionID(id).log("retrieving transaction from in-memory storage");
                    result = transactionMap.get(id);
                }
            }
            return result;
        } catch (Exception e) {
            error().transactionID(id).exception(e).log("get transaction returned an unexpected error");
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
                                        objectMapper.writeValue(output, read);
                                    }
                                }
                                result = true;
                            }
                        } else {
                            // do nothing
                        }
                    } catch (IOException exception) {
                        error().transactionID(transactionId)
                                .exception(exception)
                                .log("tryUpdateAsync: unexpected error encountered");
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

                    info().transactionID(transaction.id()).data("path", json.toString()).log("writing transaction file");

                    try (OutputStream output = Files.newOutputStream(json)) {
                        objectMapper.writeValue(output, read);
                        info().log("writing transaction file completed successfully");
                    } catch (Exception e) {
                        error().transactionID(transaction.id()).data("path", json.toString())
                                .log("error while writing transaction to file");
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
                error().transactionID(transaction.id()).data("path", path.toString()).log("content path does not exist");
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
     * Marshals {@link Transaction} to a Json file.
     *
     * @param transaction The {@link Transaction}.
     * @throws IOException If IO error occurs when saving/updating the file.
     */
    public static void marshallTransaction(Transaction transaction) throws IOException {
        // Generate the file structure
        Path path = path(transaction.id());
        Path json = path.resolve(JSON);
        try (OutputStream output = Files.newOutputStream(json)) {
            objectMapper.writeValue(output, transaction);
        }
    }
    public static Path getTransactionStorePath(){
        return transactionStore;
    }
    /**
     * Determines the directory for a {@link Transaction}.
     *
     * @param id The {@link Transaction} ID.
     * @return The {@link Path} for the specified transaction, or null if the ID is blank.
     * @throws IOException If an error occurs in determining the path.
     */
    public static Path path(String id) throws IOException {
        Path result = null;
        if (StringUtils.isNotBlank(id)) {
            result = transactionStore.resolve(id);
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
/*    static Path store() throws IOException {
        LogBuilder logBuilder = LogBuilder.logBuilder();

        if (transactionStore == null) {

            // Production configuration
            String transactionStorePath = AppConfiguration.transactionStore();
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
    }*/
}
