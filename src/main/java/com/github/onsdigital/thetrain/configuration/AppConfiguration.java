package com.github.onsdigital.thetrain.configuration;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.onsdigital.thetrain.logging.TrainEvent.info;
import static java.lang.String.format;

/**
 * Object providing access to the application configuration values. AppConfiguration is a lazy loaded singleton - use
 * {{@link #get()}} to load (if not already loaded) and get the config.
 */
public class AppConfiguration {

    private static AppConfiguration INSTANCE = null;

    public static final String TRANSACTION_STORE_ENV_KEY = "TRANSACTION_STORE";
    public static final String WEBSITE_ENV_KEY = "WEBSITE";
    public static final String THREAD_POOL_SIZE_ENV_KEY = "PUBLISHING_THREAD_POOL_SIZE";
    public static final String PORT_ENV_KEY = "PORT";
    public static final String ENABLE_VERIFY_PUBLISH_CONTENT = "ENABLE_VERIFY_PUBLISH_CONTENT";
    public static final String FILE_UPLOADS_TMP_DIR = "FILE_UPLOADS_TMP_DIR";
    public static final String MAX_FILE_UPLOAD_SIZE_MB = "MAX_FILE_UPLOAD_SIZE_MB";
    public static final String MAX_REQUEST_SIZE_MB = "MAX_REQUEST_SIZE_MB";
    public static final String FILE_THRESHOLD_SIZE_MB = "FILE_THRESHOLD_SIZE_MB";

    private Path transactionStore;
    private Path websitePath;
    private Path fileUploadsTmpDir;
    private int publishThreadPoolSize;
    private int port;
    private boolean enableVerifyPublish;
    private long maxFileUploadSize;
    private long maxRequestSize;
    private int fileThresholdSize;

    /**
     * @throws ConfigurationException
     */
    private AppConfiguration() throws ConfigurationException {
        this.transactionStore = loadTransactionStoreConfig();
        this.websitePath = loadWebsitePathConfig();
        this.enableVerifyPublish = loadEnableVerifyPublishContentFeatureFlag();
        this.fileUploadsTmpDir = createTmpFileUploadsDir();
        this.publishThreadPoolSize = getIntegerEnvVar(THREAD_POOL_SIZE_ENV_KEY);
        this.port = getIntegerEnvVar(PORT_ENV_KEY);
        this.maxFileUploadSize = getLongEnvVar(MAX_FILE_UPLOAD_SIZE_MB);
        this.maxRequestSize = getLongEnvVar(MAX_REQUEST_SIZE_MB);
        this.fileThresholdSize = getIntegerEnvVar(FILE_THRESHOLD_SIZE_MB);

        info().data(TRANSACTION_STORE_ENV_KEY, transactionStore)
                .data(WEBSITE_ENV_KEY, websitePath)
                .data(THREAD_POOL_SIZE_ENV_KEY, publishThreadPoolSize)
                .data(PORT_ENV_KEY, port)
                .data(ENABLE_VERIFY_PUBLISH_CONTENT, enableVerifyPublish)
                .data(FILE_UPLOADS_TMP_DIR, fileUploadsTmpDir)
                .data(MAX_FILE_UPLOAD_SIZE_MB, maxFileUploadSize)
                .data(MAX_REQUEST_SIZE_MB, maxRequestSize)
                .data(FILE_THRESHOLD_SIZE_MB, FileUtils.byteCountToDisplaySize(fileThresholdSize))
                .log("successfully load application configuration");
    }

    /**
     * @return the transaction store directory.
     */
    public Path transactionStore() {
        return transactionStore;
    }

    /**
     * @return the size of the publisher thread pool
     */
    public int publishThreadPoolSize() {
        return publishThreadPoolSize;
    }

    /**
     * @return the content dir path of the the website.
     */
    public Path websitePath() {
        return this.websitePath;
    }

    /**
     * @return the port to run the application on.
     */
    public int port() {
        return port;
    }

    /**
     * Is the Veify Publish feature flag enabled?
     *
     * @return true if the feature is enabled false otherwise.
     */
    public boolean isVerifyPublishEnabled() {
        return enableVerifyPublish;
    }

    /**
     * The path to the temp dir where large multipart file uploads are written.
     *
     * @return the {@link Path} to the dir.
     */
    public Path fileUploadsTmpDir() {
        return fileUploadsTmpDir;
    }

    /**
     * The maximum file upload size allowed.
     *
     * @return The maximum file upload size allowed in Bytes. A value of -1 indicates unlimited.
     */
    public long maxFileUploadSize() {
        return maxFileUploadSize;
    }

    /**
     * The maximum request size allowed.
     *
     * @return The maximum request size allowed in Bytes. A value of -1 indicates unlimited.
     */
    public long maxRequestSize() {
        return maxRequestSize;
    }

    /**
     * The threshold size at which file uploads will be written to temp files on disk. Uploads smaller than this will
     * be held in memory. A threshold of 0 means all file uploads will be written to temp disk storage.
     *
     * @return the file threshold size in MB.
     */
    public int fileThresholdSize() {
        return fileThresholdSize;
    }

    /**
     * Return a singleton instance of the ApplicationConfiguration. Will load the ApplictionConfiguration if it has
     * not already been loaded.
     *
     * @return the application configuration.
     * @throws ConfigurationException any errors while attempting to load the configuration.
     */
    public static AppConfiguration get() throws ConfigurationException {
        if (INSTANCE == null) {
            synchronized (AppConfiguration.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AppConfiguration();
                }
            }
        }
        return INSTANCE;
    }

    private static Path loadTransactionStoreConfig() throws ConfigurationException {
        String value = System.getenv(TRANSACTION_STORE_ENV_KEY);

        if (StringUtils.isEmpty(value)) {
            throw new ConfigurationException("transaction store path config is null/empty");
        }

        Path transactionStorePath = Paths.get(value);

        if (Files.notExists(transactionStorePath)) {
            throw new ConfigurationException("configured transaction store path does not exist");
        }

        if (!Files.isDirectory(transactionStorePath)) {
            throw new ConfigurationException("configured transaction store path is not a directory");
        }
        return transactionStorePath;
    }

    private static boolean loadEnableVerifyPublishContentFeatureFlag() {
        String isEnableVerifyPublish = System.getenv(ENABLE_VERIFY_PUBLISH_CONTENT);
        return Boolean.valueOf(isEnableVerifyPublish);
    }

    private static Path loadWebsitePathConfig() throws ConfigurationException {
        String value = System.getenv(WEBSITE_ENV_KEY);

        if (StringUtils.isEmpty(value)) {
            throw new ConfigurationException("website path config is null/empty");
        }

        Path websitePath = Paths.get(value);

        if (Files.notExists(websitePath)) {
            throw new ConfigurationException("configured website path does not exist");
        }

        if (!Files.isDirectory(websitePath)) {
            throw new ConfigurationException("configured website path is not a directory");
        }
        return websitePath;
    }

    private static Path createTmpFileUploadsDir() throws ConfigurationException {
        try {
            Path p = Files.createTempDirectory("tmp");
            p.toFile().deleteOnExit();
            return p;
        } catch (Exception ex) {
            throw new ConfigurationException("error creating tmp file uploads dir", ex);
        }
    }

    private static int getIntegerEnvVar(String varName) throws ConfigurationException {
        if (StringUtils.isEmpty(varName)) {
            throw new ConfigurationException("expected env var name but provided value was empty");
        }

        String value = System.getenv(varName);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new ConfigurationException(formatParsingError(varName, value, Integer.class), ex);
        }
    }

    private static long getLongEnvVar(String varName) throws ConfigurationException {
        if (StringUtils.isEmpty(varName)) {
            throw new ConfigurationException("expected env var name but provided value was empty");
        }

        String value = System.getenv(varName);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new ConfigurationException(formatParsingError(varName, value, Long.class), ex);
        }
    }

    private static final String formatParsingError(String name, String value, Class type) {
        return format("environment variable %s value %s could not be parsed to %s", name, value,
                type.getTypeName());
    }
}