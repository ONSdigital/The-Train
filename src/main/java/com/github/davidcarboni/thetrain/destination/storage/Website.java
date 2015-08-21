package com.github.davidcarboni.thetrain.destination.storage;

import com.github.davidcarboni.thetrain.destination.helpers.Configuration;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Works out the directory that contains web content so that files can be published on transaction commit.
 */
public class Website {

    static final String WEBSITE = "content_url";
    static Path path;

    /**
     * Determines the {@link Path} to the website content.
     * For development purposes, if no {@value #WEBSITE} configuration value is set
     * then a temponary folder is created.
     *
     * @return A path to the website root or, if the determined path does not point to a directory, null.
     * @throws IOException
     */
    public static Path path() throws IOException {
        Path result = null;

        // Get the Path to the website folder we're going to publish to
        if (path == null) {
            String websitePath = Configuration.get(WEBSITE);
            if (StringUtils.isNotBlank(websitePath)) {
                path = Paths.get(websitePath);
            } else {
                path = Files.createTempDirectory("website");
                System.out.println("Simulating website for development using a temp folder at: " + path);
            }
        }

        if (Files.isDirectory(path)) {
            result = path;
        } else {
            System.out.println("The configured website path is not a directory: " + path);
        }

        return result;
    }

}
