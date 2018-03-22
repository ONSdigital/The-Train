package com.github.davidcarboni.thetrain.service;

import com.github.davidcarboni.thetrain.json.Transaction;

import java.io.IOException;

public interface TransactionsService {

    Transaction create(String encryptionPassword) throws IOException;

    Transaction get(String id, String encryptionPassword) throws IOException;

    void update(Transaction transaction) throws IOException;
}
