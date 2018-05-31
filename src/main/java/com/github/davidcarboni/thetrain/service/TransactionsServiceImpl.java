package com.github.davidcarboni.thetrain.service;

import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.davidcarboni.thetrain.storage.Transactions;

import java.io.IOException;

public class TransactionsServiceImpl implements TransactionsService {

    @Override
    public Transaction create() throws IOException {
        return Transactions.create();
    }

    @Override
    public Transaction get(String id) throws IOException {
        return Transactions.get(id);
    }

    @Override
    public void update(Transaction transaction) throws IOException {
        Transactions.update(transaction);
    }

    @Override
    public void listFiles(Transaction transaction) throws IOException {
        Transactions.listFiles(transaction);
    }
}
