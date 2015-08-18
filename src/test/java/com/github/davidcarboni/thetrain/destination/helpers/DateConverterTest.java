package com.github.davidcarboni.thetrain.destination.helpers;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by david on 03/08/2015.
 */
public class DateConverterTest {

    @Test
    public void shouldConvertGmt() throws Exception {

        // Given
        // A date in Winter
        String date = "2016-12-19T18:13:33.080+0000";

        // When
        // We convert to a date and back:
        String reconverted = DateConverter.toString(DateConverter.toDate(date));

        // Then
        // The String representation should be correct:
        Assert.assertEquals(date, reconverted);
    }

    @Test
    public void shouldConvertBst() throws Exception {

        // Given
        // A date in summer
        String date = "2015-06-16T17:49:06.264+0100";

        // When
        // We convert to a date and back:
        String reconverted = DateConverter.toString(DateConverter.toDate(date));

        // Then
        // The String representation should be correct:
        Assert.assertEquals(date, reconverted);
    }

    public static void main(String[] args) {
    }
}