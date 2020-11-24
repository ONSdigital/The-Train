package com.github.onsdigital.thetrain;

import com.github.onsdigital.thetrain.configuration.AppConfiguration;
import com.github.onsdigital.thetrain.helpers.uploads.CloseablePartSupplier;
import com.github.onsdigital.thetrain.helpers.uploads.FilePartSupplier;
import com.github.onsdigital.thetrain.response.JsonTransformer;
import com.github.onsdigital.thetrain.service.ContentService;
import com.github.onsdigital.thetrain.service.ContentServiceImpl;
import com.github.onsdigital.thetrain.service.PublisherService;
import com.github.onsdigital.thetrain.service.PublisherServiceImpl;
import com.github.onsdigital.thetrain.service.TransactionsService;
import com.github.onsdigital.thetrain.service.TransactionsServiceImpl;
import com.github.onsdigital.thetrain.storage.Publisher;
import spark.ResponseTransformer;

import java.nio.file.Path;

public class Beans {

    private ResponseTransformer responseTransformer;
    private TransactionsService transactionsService;
    private PublisherService publisherService;
    private ContentService contentService;
    private CloseablePartSupplier filePartSupplier;
    private Path websitePath;

    public Beans(AppConfiguration cfg) {
        this.websitePath = cfg.websitePath();
        this.responseTransformer = JsonTransformer.get();
        this.transactionsService = new TransactionsServiceImpl();

        this.filePartSupplier = new FilePartSupplier(cfg.fileUploadsTmpDir(), cfg.maxFileUploadSize(),
                cfg.maxRequestSize(), cfg.fileThresholdSize());

        this.publisherService = new PublisherServiceImpl(Publisher.getInstance(), websitePath);
        this.contentService = new ContentServiceImpl(transactionsService);
    }

    public ResponseTransformer getResponseTransformer() {
        return responseTransformer;
    }

    public CloseablePartSupplier getFilePartSupplier() {
        return filePartSupplier;
    }

    public TransactionsService getTransactionsService() {
        return transactionsService;
    }

    public PublisherService getPublisherService() {
        return publisherService;
    }

    public ContentService getContentService() {
        return contentService;
    }

    public Path getWebsitePath() {
        return websitePath;
    }
}
