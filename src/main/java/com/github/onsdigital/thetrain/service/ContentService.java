package com.github.onsdigital.thetrain.service;

import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.json.Transaction;

import java.io.IOException;

public interface ContentService {

    String getContentHash(Transaction transaction, String uri) throws IllegalArgumentException, IOException, PublishException;

}
