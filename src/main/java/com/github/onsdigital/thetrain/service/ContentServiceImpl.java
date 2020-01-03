package com.github.onsdigital.thetrain.service;

import com.github.onsdigital.thetrain.helpers.PathUtils;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.storage.Transactions;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ContentServiceImpl implements ContentService {

    @Override
    public boolean isValidHash(Transaction transaction, String uri, String expectedHash) throws
            IllegalArgumentException, IOException {
        validateArgs(transaction, uri, expectedHash);

        Path contentPath = getContentPath(transaction, uri);

        checkContentExists(contentPath);

        String actualHash = getActualHashForContent(contentPath);

        return StringUtils.equals(expectedHash, actualHash);
    }

    private void validateArgs(Transaction transaction, String uri, String expectedHash) {
        if (transaction == null) {
            throw new IllegalArgumentException("isContentHashCorrect requires transaction but was null");
        }

        if (StringUtils.isEmpty(uri)) {
            throw new IllegalArgumentException("isContentHashCorrect requires uri but none provided");
        }

        if (StringUtils.isEmpty(expectedHash)) {
            throw new IllegalArgumentException("isContentHashCorrect requires expectedHash value but none provided");
        }
    }

    private Path getContentPath(Transaction transaction, String uri) throws IOException {
        Path content = Transactions.content(transaction);
        return PathUtils.toPath(uri, content);
    }

    private void checkContentExists(Path contentPath) {
        if (Files.notExists(contentPath)) {
            throw new ContentException("isContentHashCorrect requested URI does not exist");
        }
    }

    private String getActualHashForContent(Path contentPath) throws IOException {
        String actualHash = "";

        try (InputStream in = Files.newInputStream(contentPath)) {
            actualHash = DigestUtils.sha1Hex(in);
        }

        return actualHash;
    }
}
