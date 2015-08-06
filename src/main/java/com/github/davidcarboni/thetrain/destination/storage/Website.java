package com.github.davidcarboni.thetrain.destination.storage;

import com.github.davidcarboni.thetrain.destination.helpers.Configuration;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by david on 05/08/2015.
 */
public class Website {

    static final String WEBSITE = "thetrain.website";
    static Path path;

    public static Path path() throws IOException {
        Path result = null;

        // Get the Path to the website folder we're going to publish to
        if (path == null) {
            String websitePath = Configuration.get(WEBSITE);
            if (StringUtils.isNotBlank(websitePath)) {
                path = Paths.get(websitePath);
            } else {
                path = Files.createTempDirectory("website");
                System.out.println("Simulating website for development using a temp folder: " + path);
            }
        }

        if (Files.isDirectory(path)) {
            result = path;
        }

        return result;
    }

}
