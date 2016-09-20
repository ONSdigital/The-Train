package com.github.davidcarboni.thetrain.json.request;

import java.util.ArrayList;
import java.util.List;

public class Manifest {
    public List<FileCopy> filesToCopy;
    public List<String> urisToDelete;

    public void addUriToDelete(String uri) {
        if (urisToDelete == null)
            urisToDelete = new ArrayList<>();

        urisToDelete.add(uri);
    }
}
