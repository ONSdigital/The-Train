package com.github.davidcarboni.thetrain.service;

import com.github.davidcarboni.thetrain.json.Transaction;

import java.io.IOException;
import java.nio.file.Path;

public interface PublisherService {

    Path websitePath() throws IOException;

    boolean commit(Transaction transaction, Path website) throws IOException;
}
