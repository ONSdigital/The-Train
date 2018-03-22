package com.github.davidcarboni.thetrain.service;

import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.davidcarboni.thetrain.storage.Transactions;

import java.io.IOException;

public class TransactionsServiceImpl implements TransactionsService {

    @Override
    public Transaction create(String encryptionPassword) throws IOException {
        return Transactions.create(encryptionPassword);
    }

    @Override
    public Transaction get(String id, String encryptionPassword) throws IOException {
        return Transactions.get(id, encryptionPassword);
    }

    @Override
    public void update(Transaction transaction) throws IOException {
        Transactions.update(transaction);
    }
}
