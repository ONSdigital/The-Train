package com.github.davidcarboni.thetrain.destination.helpers;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Created by david on 18/08/2015.
 */
public class PathUtilsTest {

    Path tempFolder;

    @Before
    public void beforeTest() throws IOException {
        tempFolder = Files.createTempDirectory(this.getClass().getSimpleName());
    }

    @Test
    public void shouldFindContainedPath() throws IOException {

        // Given
        // A contained folder
        Path container = folder("container");
        Path contained = folder("container/sub/folder");
        Files.createDirectories(contained)  ;

        // When
        // We check if the path is contained
        boolean result = PathUtils.isContained(contained, container);

        // Then
        // The answer should be yes
        assertTrue(result);
    }

    @Test
    public void shouldNotFindNotContainedPath() throws IOException {

        // Given
        // A contained folder
        Path container = folder("container");
        Path contained = folder("some/other/folder");

        // When
        // We check if the path is contained
        boolean result = PathUtils.isContained(contained, container);

        // Then
        // The answer should be yes
        assertFalse(result);
    }

    @Test
    public void shouldNotFindNotContainedRelativePath() throws IOException {

        // Given
        // A contained folder
        Path container = folder("container");
        Path contained = folder("container/../parent/level/folder");

        // When
        // We check if the path is contained
        boolean result = PathUtils.isContained(contained, container);

        // Then
        // The answer should be yes
        assertFalse(result);
    }

    @Test
    public void shouldNotFindDodgyRelativePath() throws IOException {

        // Given
        // A contained folder
        Path container = folder("series/of/folders");
        Path contained = folder("series/of/folders/.././././../../dodgy/relative/path");

        // When
        // We check if the path is contained
        boolean result = PathUtils.isContained(contained, container);

        // Then
        // The answer should be yes
        assertFalse(result);
    }

    private  Path folder(String path) throws IOException {
        Path result = tempFolder.resolve(path);
        Files.createDirectories(result);
        return result;
    }

}