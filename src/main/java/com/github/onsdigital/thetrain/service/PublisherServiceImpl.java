package com.github.onsdigital.thetrain.service;

import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.storage.Publisher;
import com.github.onsdigital.thetrain.storage.Website;

import java.io.IOException;
import java.nio.file.Path;

public class PublisherServiceImpl implements PublisherService {

    @Override
    public Path websitePath() throws IOException {
        return Website.path();
    }

    @Override
    public boolean commit(Transaction transaction, Path website) throws IOException {
        return Publisher.getInstance().commit(transaction, website);
    }

    @Override
    public boolean rollback(Transaction transaction) throws IOException {
        return Publisher.getInstance().rollback(transaction);
    }
}
