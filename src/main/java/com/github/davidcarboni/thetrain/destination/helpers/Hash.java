package com.github.davidcarboni.thetrain.destination.helpers;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by david on 05/08/2015.
 */
public class Hash {

    public static String hash(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return DigestUtils.sha512Hex(input);
        }
    }
}
