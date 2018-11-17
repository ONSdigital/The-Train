package com.github.onsdigital.thetrain.service;

import com.github.onsdigital.thetrain.json.Transaction;

import java.io.IOException;

public interface TransactionsService {

    Transaction create() throws IOException;

    Transaction get(String id) throws IOException;

    void update(Transaction transaction) throws IOException;

    void listFiles(Transaction transaction) throws IOException;
}
