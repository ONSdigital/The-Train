package com.github.davidcarboni.thetrain.destination.helpers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by david on 18/08/2015.
 */
public class PathUtils {

    /**
     *
     * Inspired by <a href="http://stackoverflow.com/questions/18227634/check-if-file-is-in-subdirectory"
     * >http://stackoverflow.com/questions/18227634/check-if-file-is-in-subdirectory</a>
     *
     * @param contained   The path to be checked. This does not need to exist.
     * @param container The parent directory. This must exist on the filesystem.
     * @return If <code>contained</code> is a subfolder of <code>container</code>, true.
     * @throws IOException If an error occurs or, potentially, if either path does not exist.
     */
    public static boolean isContained(Path contained, Path container) throws IOException {
        Path current = contained.normalize();

        // Iterate up the path to see if we find the container path:
        while (current != null) {
            if (Files.isSameFile(container, current)) {
                return true;
            }
            current = current.getParent();
        }

        // If we didn't find the container path
        // amongst the parents of the contained path,
        // this path is not contained:
        return false;
    }
}
