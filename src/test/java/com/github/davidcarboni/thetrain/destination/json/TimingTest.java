package com.github.davidcarboni.thetrain.destination.json;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by david on 04/08/2015.
 */
public class TimingTest {

    @Test
    public void shouldSetStartDateAnUriOnInstantiation() throws InterruptedException {

        // Given
        String uri = "/uri";

        // When
        Timing timing = new Timing(uri);

        // Then
        Assert.assertNotNull(timing.startDate);
        Assert.assertEquals(timing.startDate, DateConverter.toDate(timing.start));
        Assert.assertNull(timing.endDate);
        Assert.assertEquals(timing.duration, 0);
    }

    @Test
    public void shouldSetEndDateAndDurationOnStop() throws InterruptedException {

        // Given
        Timing timing = new Timing("test");
        Thread.sleep(2);

        // When
        timing.stop();

        // Then
        Assert.assertNotNull(timing.endDate);
        Assert.assertEquals(timing.endDate, DateConverter.toDate(timing.end));
        Assert.assertTrue(timing.duration > 0);
    }
}