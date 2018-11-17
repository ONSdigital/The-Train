package com.github.onsdigital.thetrain.service;

import com.github.onsdigital.thetrain.json.Transaction;

import java.io.IOException;
import java.nio.file.Path;

public interface PublisherService {

    Path websitePath() throws IOException;

    boolean commit(Transaction transaction, Path website) throws IOException;

    boolean rollback(Transaction transaction) throws IOException;
}
