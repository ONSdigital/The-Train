package com.github.onsdigital.thetrain.routes;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.helpers.Hash;
import com.github.onsdigital.thetrain.helpers.PathUtils;
import com.github.onsdigital.thetrain.helpers.uploads.CloseablePart;
import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.storage.Publisher;
import com.github.onsdigital.thetrain.storage.PublisherTest;
import com.github.onsdigital.thetrain.storage.Transactions;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import spark.Route;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.onsdigital.thetrain.routes.CommitTransaction.RESULT_SUCCESS_MSG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SendManifestTest extends BaseRouteTest {

    private Route route;
    private String testURI = "/a/b/c";

    private Path websiteTestPath;

    @Mock
    private CloseablePart closeablePart;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private BadRequestException badRequestException;
    private PublishException publishException;
    private Path websitePath = Paths.get("/website/path");

    @BeforeClass
    public static void preSetup() throws Exception {
        Publisher.init(20);
    }

    @Override
    public void customSetUp() throws Exception {
        websiteTestPath = Files.createTempDirectory("website");
        Path transactionStorePath = Files.createTempDirectory("transaction-store");
        Path transactionArchiveStorePath = Files.createTempDirectory("transaction-archive-store");

        Transactions.init(transactionStorePath, transactionArchiveStorePath);
        transaction = Transactions.create();
        route = new GetTransaction(transactionsService);
        route = new AddFileToTransaction(transactionsService, publisherService, filePartSupplier);

//        //Setting up the transaction folder
//        websiteTestPath = Files.createTempDirectory("website");
//        Path transactionStorePath = Files.createTempDirectory("transaction-store");

        //Initialises and creates the transaction folder
//        Transactions.init(transactionStorePath);
//        transaction = Transactions.create();

        //Setting up for a commit.
        route = new CommitTransaction(transactionsService, publisherService);
        route = new AddFileToTransaction(transactionsService, publisherService, filePartSupplier);
    }

    @Test
    public void testCheckMarshalWasSuccessfulAfterCommit() throws Exception {

        //Commit a Transaction
        when(transactionsService.getTransaction(request)).thenReturn(transaction);
//        when(publisherService.commit(transaction)).thenReturn(true);

//        if (publisherService.commit(transaction)) {
        verify(publisherService,times(1)).commit(transaction);

            // check the transaction was saved correctly
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
            objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            Transaction unmarshalledTransaction = null;

            //Get the path of where the Transactions are located.
            Path transactionPath = null;
            if (StringUtils.isNotBlank(transaction.id())) {
                final Path transactionStorePath = Transactions.getTransactionStorePath();
                transactionPath = transactionStorePath.resolve(transaction.id());
            }

            //Test whether the values in transaction.json are written to disk are as expected.
            if (StringUtils.isNotBlank(transaction.id())) {
                System.out.println("request.raw().getParameter(\"transactionId\") = " + request.raw().getParameter("transactionId"));
                System.out.println("transaction.id() = " + transaction.id());
                if (transactionPath != null && Files.exists(transactionPath)) {
                    final Path json = transactionPath.resolve("transaction.json");
                    try (InputStream input = Files.newInputStream(json)) {
                        unmarshalledTransaction = objectMapper.readValue(input, Transaction.class);
                    }
                    assertThat(unmarshalledTransaction.getStatus(), equalTo("committed"));
                } else {
                    //Need to do something here.
                }
            } else {
                fail("transaction ID was blank");
            }

//            verify(transactionsService, times(1)).getTransaction(request);
//            verify(transactionsService, times(1)).update(transaction);
//            verify(publisherService, times(1)).commit(transactions);

//        } else {
//            fail("commit returned false, instead of true");
//        }
    }

    private static Path tempFile() throws IOException {

        // A temp file
        Path file = Files.createTempFile(PublisherTest.class.getSimpleName(), ".txt");

        try (InputStream input = Random.inputStream(5000); OutputStream output = Files.newOutputStream(file)) {
            IOUtils.copy(input, output);
        }

        return file;
    }

    @Test
    public void testFileCopySuccessfulORIG() throws Exception {

        // Given
        // A URI to copy to with an existing published file.
        String uri = "/test.txt";
        Files.move(tempFile(), PathUtils.toPath(uri, websiteTestPath)); // create published file in website directory

        // When
        // We publish the file
        Publisher.getInstance().addFile(transaction, uri, Random.inputStream(5000), websiteTestPath);

        // Then
        // The transaction should exist and be populated with values
        Path path = Publisher.getInstance().getFile(transaction, uri);
        assertTrue(transaction.getStatus().equals(Transaction.PUBLISHING));
        Assert.assertNotNull(path);

        // there is a file in the backup directory that is the same as the website file
        Path backup = Transactions.backup(transaction);
        assertTrue(Files.exists(PathUtils.toPath(uri, backup)));
        Assert.assertEquals(Hash.sha(PathUtils.toPath(uri, backup)),
                Hash.sha(PathUtils.toPath(uri, websiteTestPath)));
    }

    @Test
    public void testFileCopyUnSuccessful() throws Exception {

        // Given
        // A URI to copy to with an existing published file.
        String uri = "/test.txt";
        Files.move(tempFile(), PathUtils.toPath(uri, websiteTestPath)); // create published file in website directory

        // When
        // We publish the file
        Publisher.getInstance().addFile(transaction, uri, Random.inputStream(5000), websiteTestPath);

        // Then
        // The transaction should exist and be populated with values
        Path path = Publisher.getInstance().getFile(transaction, uri);

        // And
        // The file is then removed
        Files.delete(PathUtils.toPath(uri, websiteTestPath).toAbsolutePath());

        assertTrue(transaction.getStatus().equals(Transaction.PUBLISHING));
        Assert.assertNotNull(path);

        // there is a file in the backup directory that is the same as the website file
        Path backup = Transactions.backup(transaction);
        assertTrue(Files.exists(PathUtils.toPath(uri, backup)));
        Assert.assertEquals(Hash.sha(PathUtils.toPath(uri, backup)),
                Hash.sha(PathUtils.toPath(uri, websiteTestPath)));
    }


    @Test
    public void testCommitTransactionSuccess() throws Exception {
        System.out.println("transaction.id() = " + transaction.id());
        when(transactionsService.getTransaction(request)).thenReturn(transaction);

        when(publisherService.commit(transaction)).thenReturn(true);

        Result result = (Result) route.handle(request, response);

        assertThat(result.transaction, equalTo(transaction));
        assertThat(result.message, equalTo(RESULT_SUCCESS_MSG));
        assertFalse(result.error);


        verify(transactionsService, times(1)).getTransaction(request);
        verify(transactionsService, times(1)).update(transaction);
        verify(publisherService, times(1)).commit(transaction);
    }


//    private  static Transaction getTransactionFromFile(String id) throws IOException {
//
//        Path transactionPath
//        if (StringUtils.isNotBlank(id)) {
//            transactionPath = transactionStore.resolve(id);
//        }
//
//         = path(id);
//        if (transactionPath != null && Files.exists(transactionPath)) {
//            final Path json = transactionPath.resolve(JSON);
//            try (InputStream input = Files.newInputStream(json)) {
//                result = objectMapper.readValue(input, Transaction.class);
//            }
//        }
//
//
//
//
//
//        Transaction result = null;
//        info().transactionID(id)
//                .log("attempting to read tansaction from file system");
//        // Generate the file structure
//        Path transactionPath = path(id);
//        if (transactionPath != null && Files.exists(transactionPath)) {
//            final Path json = transactionPath.resolve(JSON);
//            try (InputStream input = Files.newInputStream(json)) {
//                result = objectMapper.readValue(input, Transaction.class);
//            }
//        }
//        return result;
//    }
}
