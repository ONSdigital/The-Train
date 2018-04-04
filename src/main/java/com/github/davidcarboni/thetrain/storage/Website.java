package com.github.davidcarboni.thetrain.storage;

import com.github.davidcarboni.thetrain.helpers.Configuration;
import com.github.davidcarboni.thetrain.logging.LogBuilder;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.davidcarboni.thetrain.logging.LogBuilder.logBuilder;

/**
 * Works out the directory that contains web content so that files can be published on transaction commit.
 */
public class Website {

    static Path path;

    /**
     * Determines the {@link Path} to the website content.
     * For development purposes, if no {@value Configuration#WEBSITE} configuration value is set
     * then a temponary folder is created.
     *
     * @return A path to the website root or, if the determined path does not point to a directory, null.
     * @throws IOException
     */
    public static Path path() throws IOException {
        LogBuilder logBuilder = logBuilder();
        Path result = null;

        // Get the Path to the website folder we're going to publish to
        if (path == null) {
            String websitePath = Configuration.website();
            if (StringUtils.isNotBlank(websitePath)) {
                path = Paths.get(websitePath);
                logBuilder.addParameter("websitePath", path.toString())
                        .info("WEBSITE configured");
            } else {
                path = Files.createTempDirectory("website");
                logBuilder.addParameter("path", path.toString())
                        .info("simulating website for development using a temp folder");
            }
        }

        if (Files.isDirectory(path)) {
            result = path;
        } else {
            logBuilder.addParameter("path", path.toString())
                    .info("the configured website path is not a directory");
        }

        return result;
    }

}
