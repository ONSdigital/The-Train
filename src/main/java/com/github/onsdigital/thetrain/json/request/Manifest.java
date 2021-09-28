package com.github.onsdigital.thetrain.json.request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Manifest {
    private List<FileCopy> filesToCopy;
    private List<String> urisToDelete;

    public void addUriToDelete(String uri) {
        if (urisToDelete == null)
            urisToDelete = new ArrayList<>();

        urisToDelete.add(uri);
    }

    public List<String> getUrisToDelete() {
        return this.urisToDelete;
    }

    public List<FileCopy> getFilesToCopy() {
        return this.filesToCopy;
    }

    public void addFilesToCopy(FileCopy ... fc) {
        filesToCopy.addAll(Arrays.asList(fc));
    }
}
