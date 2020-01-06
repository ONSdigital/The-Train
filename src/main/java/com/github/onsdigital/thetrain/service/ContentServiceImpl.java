package com.github.onsdigital.thetrain.service;

import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.helpers.PathUtils;
import com.github.onsdigital.thetrain.json.Transaction;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ContentServiceImpl implements ContentService {

    private TransactionsService transactionsService;

    public ContentServiceImpl(TransactionsService transactionsService) {
        this.transactionsService = transactionsService;
    }

    @Override
    public String getContentHash(Transaction transaction, String uri) throws PublishException, IOException {
        validateArgs(transaction, uri);

        Path contentPath = getContentPath(transaction, uri);

        checkContentExists(contentPath);

        return getContentHash(contentPath);
    }

    private void validateArgs(Transaction transaction, String uri) {
        if (transaction == null) {
            throw new IllegalArgumentException("transaction required but was null");
        }

        if (StringUtils.isEmpty(uri)) {
            throw new IllegalArgumentException("uri required but none provided");
        }
    }

    private Path getContentPath(Transaction transaction, String uri) throws PublishException {
        Path content = transactionsService.content(transaction);
        return PathUtils.toPath(uri, content);
    }

    private void checkContentExists(Path contentPath) {
        if (Files.notExists(contentPath)) {
            throw new ContentException("no context exists for the requested URI in this transaction");
        }
    }

    private String getContentHash(Path contentPath) throws IOException {
        String actualHash = "";

        try (
                InputStream in = Files.newInputStream(contentPath);
                BufferedInputStream bin = new BufferedInputStream(in)
        ) {
            actualHash = DigestUtils.sha1Hex(bin);
        }

        return actualHash;
    }
}
