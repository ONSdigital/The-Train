package com.github.onsdigital.thetrain;

import com.github.onsdigital.thetrain.json.Transaction;
import com.github.onsdigital.thetrain.service.ContentService;
import com.github.onsdigital.thetrain.service.ContentServiceImpl;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class ContentServiceImplTest {

    private ContentService service;

    @Before
    public void setUp() {
        service = new ContentServiceImpl();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVerifyMD5Hash_transactionNull() throws Exception {
        try {
            service.isValidHash(null, null, null);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("verify MD5 hash requires transaction but was null"));
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVerifyMD5Hash_uriNull() throws Exception {
        try {
            service.isValidHash(new Transaction(), null, null);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("verify MD5 hash requires uri but none provided"));
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVerifyMD5Hash_uriEmpty() throws Exception {
        try {
            service.isValidHash(new Transaction(), "", null);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("verify MD5 hash requires uri but none provided"));
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVerifyMD5Hash_expectedMD5Null() throws Exception {
        try {
            service.isValidHash(new Transaction(), "/a/b/c", null);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("verify MD5 hash requires expectedMD5 but none provided"));
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVerifyMD5Hash_expectedMD5Empty() throws Exception {
        try {
            service.isValidHash(new Transaction(), "/a/b/c", "");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("verify MD5 hash requires expectedMD5 but none provided"));
            throw ex;
        }
    }
}
