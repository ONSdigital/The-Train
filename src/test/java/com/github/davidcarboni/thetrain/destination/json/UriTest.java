package com.github.davidcarboni.thetrain.destination.json;

import com.github.davidcarboni.thetrain.destination.helpers.DateConverter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

/**
 * Test for {@link Uri}.
 */
public class UriTest {

    @Test
    public void shouldSetStartDateUriAndStatusOnInstantiation() throws InterruptedException {

        // Given
        String uriString = "/uri";

        // When
        Uri uri = new Uri(uriString, new Date());

        // Then
        Assert.assertNotNull(uri.startDate);
        Assert.assertEquals(uri.startDate, DateConverter.toDate(uri.start));
        Assert.assertNull(uri.endDate);
        Assert.assertEquals(uri.duration, 0);
        Assert.assertEquals(Uri.STARTED, uri.status);
    }

    @Test
    public void shouldSetEndDateDurationShaAndStatusOnStop() throws InterruptedException {

        // Given
        String sha = "123abc";
        Uri uri = new Uri("test", new Date());
        Thread.sleep(2);

        // When
        uri.stop(sha);

        // Then
        Assert.assertNotNull(uri.endDate);
        Assert.assertEquals(uri.endDate, DateConverter.toDate(uri.end));
        Assert.assertTrue(uri.duration > 0);
        Assert.assertEquals(sha, uri.sha);
        Assert.assertEquals(Uri.UPLOADED, uri.status);
    }

    @Test
    public void shouldNotUpdateStatusForBlankSha() throws InterruptedException {

        // Given
        Uri blank = new Uri("blank", new Date());
        Uri nul = new Uri("null", new Date());

        // When
        blank.stop("");
        nul.stop(null);

        // Then
        Assert.assertEquals("upload failed", blank.status);
        Assert.assertEquals("upload failed", nul.status);
    }

    @Test
    public void shouldEvaluateEquality() throws InterruptedException {

        // Given
        // URIs that are and are not equal
        Uri compare = new Uri("/uri", new Date());
        Uri equal = new Uri("/uri", new Date());
        Uri notEqual = new Uri("/other", new Date());

        // When
        // We test equality
        boolean equalResult = compare.equals(equal);
        boolean notEqualResult = compare.equals(notEqual);

        // Then
        // We should get equal/not equal as expected
        Assert.assertTrue(equalResult);
        Assert.assertFalse(notEqualResult);
    }
}