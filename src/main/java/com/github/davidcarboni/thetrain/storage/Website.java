package com.github.davidcarboni.thetrain.storage;

import com.github.davidcarboni.thetrain.helpers.Configuration;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.davidcarboni.thetrain.logging.LogBuilder.info;

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
        Path result = null;

        // Get the Path to the website folder we're going to publish to
        if (path == null) {
            String websitePath = Configuration.website();
            if (StringUtils.isNotBlank(websitePath)) {
                path = Paths.get(websitePath);
                info("WEBSITE configured")
                        .addParameter("websitePath", path.toString())
                        .log();
            } else {
                path = Files.createTempDirectory("website");
                info("simulating website for development using a temp folder")
                        .addParameter("path", path.toString())
                        .log();
                info("please configure a WEBSITE variable to configure this directory in production").log();
            }
        }

        if (Files.isDirectory(path)) {
            result = path;
        } else {
            info("the configured website path is not a directory")
                    .addParameter("path", path.toString())
                    .log();
        }

        return result;
    }

}
