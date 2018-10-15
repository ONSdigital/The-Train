package com.github.davidcarboni.thetrain.storage;

import com.github.davidcarboni.thetrain.json.UriInfo;

public class TransactionUpdate {

    private boolean isSuccess;
    private UriInfo uriInfo;

    public TransactionUpdate() {
        this.isSuccess = false; // default
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public void setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public UriInfo getUriInfo() {
        return uriInfo;
    }
}
