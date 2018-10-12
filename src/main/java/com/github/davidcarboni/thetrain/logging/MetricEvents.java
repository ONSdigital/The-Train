package com.github.davidcarboni.thetrain.logging;

public enum MetricEvents {

    ADD_FILES("addFiles", "adding the content of a zip file to the transaction"),

    ADD_DELETE_FILES("addDeleteFiles", "adding URIS to be deleted by this transaction"),

    APPLY_DELETES("applyDeletes", "execute the deletes fot this transaction"),

    COMMIT("commit", "commit the transaction"),

    COPY_FILE("copyFile", "copy a file into the the transaction"),

    COPY_FILES("copyFiles", "copy files into the the transaction");

    private final String name;
    private final String description;

    MetricEvents (String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
