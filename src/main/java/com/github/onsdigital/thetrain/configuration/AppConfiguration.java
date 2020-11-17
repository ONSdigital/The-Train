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
        this.publishThreadPoolSize = loadPublishPoolSizeConfig();
        this.port = loadPortConfig();
        this.enableVerifyPublish = loadEnableVerifyPublishContentFeatureFlag();
        this.fileUploadsTmpDir = createTmpFileUploadsDir();
        this.maxFileUploadSize = loadMaxFileSizeConfig();
        this.maxRequestSize = loadMaxRequestSizeConfig();
        this.fileThresholdSize = loadFileThresholdSizeConfig();

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

    public boolean isVerifyPublishEnabled() {
        return enableVerifyPublish;
    }

    public Path fileUploadsTmpDir() {
        return fileUploadsTmpDir;
    }

    public long maxFileUploadSize() {
        return maxFileUploadSize;
    }

    public long maxRequestSize() {
        return maxRequestSize;
    }

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

    private static int loadPublishPoolSizeConfig() throws ConfigurationException {
        String value = System.getenv(MAX_REQUEST_SIZE_MB);
        try {
            return Integer.parseInt(System.getenv(THREAD_POOL_SIZE_ENV_KEY));
        } catch (NumberFormatException ex) {
            throw new ConfigurationException(formatParsingError(THREAD_POOL_SIZE_ENV_KEY, value, Integer.class), ex);
        }
    }

    private static int loadPortConfig() throws ConfigurationException {
        String value = System.getenv(MAX_REQUEST_SIZE_MB);
        try {
            return Integer.parseInt(System.getenv(PORT_ENV_KEY));
        } catch (NumberFormatException e) {
            throw new ConfigurationException(formatParsingError(PORT_ENV_KEY, value, Integer.class), e);
        }
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

    private static long loadMaxFileSizeConfig() throws ConfigurationException {
        String value = System.getenv(MAX_REQUEST_SIZE_MB);
        try {
            return Long.parseLong(System.getenv(MAX_FILE_UPLOAD_SIZE_MB));
        } catch (NumberFormatException ex) {
            throw new ConfigurationException(formatParsingError(MAX_FILE_UPLOAD_SIZE_MB, value, Long.class), ex);
        }
    }

    private static long loadMaxRequestSizeConfig() throws ConfigurationException {
        String value = System.getenv(MAX_REQUEST_SIZE_MB);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new ConfigurationException(formatParsingError(MAX_REQUEST_SIZE_MB, value, Long.class), ex);
        }
    }

    private static int loadFileThresholdSizeConfig() throws ConfigurationException {
        String value = System.getenv(MAX_REQUEST_SIZE_MB);
        try {
            int mb = Integer.parseInt(System.getenv(FILE_THRESHOLD_SIZE_MB));
            return 1024 * 1024 * mb;
        } catch (NumberFormatException ex) {
            throw new ConfigurationException(formatParsingError(FILE_THRESHOLD_SIZE_MB, value, Integer.class), ex);
        }
    }

    static final String formatParsingError(String name, String value, Class type) {
        return format("environment variable %s value %s could not be parsed to %s", name, value,
                type.getTypeName());
    }
}
