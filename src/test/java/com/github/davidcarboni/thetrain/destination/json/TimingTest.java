package com.github.davidcarboni.thetrain.destination.json;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link Timing}.
 */
public class TimingTest {

    @Test
    public void shouldSetStartDateUriAndStatusOnInstantiation() throws InterruptedException {

        // Given
        String uri = "/uri";

        // When
        Timing timing = new Timing(uri);

        // Then
        Assert.assertNotNull(timing.startDate);
        Assert.assertEquals(timing.startDate, DateConverter.toDate(timing.start));
        Assert.assertNull(timing.endDate);
        Assert.assertEquals(timing.duration, 0);
        Assert.assertEquals("started", timing.status);
    }

    @Test
    public void shouldSetEndDateDurationShaAndStatusOnStop() throws InterruptedException {

        // Given
        String sha = "123abc";
        Timing timing = new Timing("test");
        Thread.sleep(2);

        // When
        timing.stop(sha);

        // Then
        Assert.assertNotNull(timing.endDate);
        Assert.assertEquals(timing.endDate, DateConverter.toDate(timing.end));
        Assert.assertTrue(timing.duration > 0);
        Assert.assertEquals(sha, timing.sha);
        Assert.assertEquals("uploaded", timing.status);
    }

    @Test
    public void shouldNotUpdateStatusForBlankSha() throws InterruptedException {

        // Given
        Timing blank = new Timing("blank");
        Timing nul = new Timing("null");

        // When
        blank.stop("");
        nul.stop(null);

        // Then
        Assert.assertEquals("upload failed", blank.status);
        Assert.assertEquals("upload failed", nul.status);
    }
}