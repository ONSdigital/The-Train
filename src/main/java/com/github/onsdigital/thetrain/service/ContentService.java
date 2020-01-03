package com.github.onsdigital.thetrain.service;

import com.github.onsdigital.thetrain.json.Transaction;

import java.io.IOException;

public interface ContentService {

    boolean isValidHash(Transaction transaction, String uri, String expectedHash) throws IllegalArgumentException,
            IOException;

}
