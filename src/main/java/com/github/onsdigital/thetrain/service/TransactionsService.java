package com.github.onsdigital.thetrain.service;

import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.json.Transaction;
import spark.Request;

import java.io.IOException;
import java.util.concurrent.Future;

public interface TransactionsService {

    Transaction create() throws PublishException;

    Transaction getTransaction(String transactionID) throws BadRequestException, PublishException;

    Transaction getTransaction(Request request) throws BadRequestException, PublishException;

    void update(Transaction transaction) throws PublishException;

    void listFiles(Transaction transaction) throws PublishException;

    Future<Boolean> tryUpdateAsync(final Transaction transaction) throws PublishException;
}
