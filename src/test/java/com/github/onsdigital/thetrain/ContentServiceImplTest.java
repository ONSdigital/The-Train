package com.github.onsdigital.thetrain;

import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.service.ContentException;
import com.github.onsdigital.thetrain.service.ContentService;
import com.github.onsdigital.thetrain.service.ContentServiceImpl;
import com.github.onsdigital.thetrain.service.TransactionsService;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class ContentServiceImplTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private TransactionsService transactionsService;

    private ContentService service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        service = new ContentServiceImpl(transactionsService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetContentHash_transactionNull() throws Exception {
        try {
            service.getContentHash(null, null);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("transaction required but was null"));
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetContentHash_uriNull() throws Exception {
        try {
            service.getContentHash(new Transaction(), null);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("uri required but none provided"));
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetContentHash_uriEmpty() throws Exception {
        try {
            service.getContentHash(new Transaction(), "");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("uri required but none provided"));
            throw ex;
        }
    }

    @Test(expected = ContentException.class)
    public void testGetContentHash_contentNotFound() throws Exception {
        File transactionDir = tempFolder.newFolder("test1");

        when(transactionsService.content(any(Transaction.class)))
                .thenReturn(transactionDir.toPath());

        service.getContentHash(new Transaction(), "a/b/c/data.json");
    }

    @Test
    public void testGetContentHash_success() throws Exception {
        File transactionDir = tempFolder.newFolder("test1");
        Path contentPath = Files.createDirectories(transactionDir.toPath().resolve("a/b/c"));
        Path content = Files.createFile(contentPath.resolve("data.json"));

        Files.write(content, "hello world".getBytes());

        String expectedHash = DigestUtils.sha1Hex("hello world".getBytes());

        when(transactionsService.content(any(Transaction.class)))
                .thenReturn(transactionDir.toPath());

        String actual = service.getContentHash(new Transaction(), "a/b/c/data.json");

        assertThat(actual, equalTo(expectedHash));
    }
}
