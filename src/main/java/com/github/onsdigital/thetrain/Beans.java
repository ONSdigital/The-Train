package com.github.onsdigital.thetrain;

import com.github.onsdigital.thetrain.helpers.FileUploadHelper;
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
    private FileUploadHelper fileUploadHelper;
    private PublisherService publisherService;
    private ContentService contentService;
    private Path websitePath;

    public Beans(Path websitePath) {
        this.websitePath = websitePath;
        this.responseTransformer = JsonTransformer.get();
        this.transactionsService = new TransactionsServiceImpl();
        this.fileUploadHelper = new FileUploadHelper();
        this.publisherService = new PublisherServiceImpl(Publisher.getInstance(), websitePath);
        this.contentService = new ContentServiceImpl(transactionsService);
    }

    public ResponseTransformer getResponseTransformer() {
        return responseTransformer;
    }

    public FileUploadHelper getFileUploadHelper() {
        return fileUploadHelper;
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
