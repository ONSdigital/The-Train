package com.github.davidcarboni.thetrain.service;

import com.github.davidcarboni.thetrain.json.Transaction;

import java.io.IOException;

public interface TransactionsService {

    Transaction create() throws IOException;

    Transaction get(String id) throws IOException;

    void update(Transaction transaction) throws IOException;

    void listFiles(Transaction transaction) throws IOException;
}
