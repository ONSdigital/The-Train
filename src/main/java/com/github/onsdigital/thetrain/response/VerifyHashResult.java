package com.github.onsdigital.thetrain.response;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class VerifyHashResult {

    private String transactionId;
    private String uri;
    private String hash;
    private boolean isValid;

    public VerifyHashResult(String transactionId, String uri, String hash, boolean isValid) {
        this.transactionId = transactionId;
        this.uri = uri;
        this.hash = hash;
        this.isValid = isValid;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getUri() {
        return uri;
    }

    public String getHash() {
        return hash;
    }

    public boolean isValid() {
        return isValid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        VerifyHashResult that = (VerifyHashResult) o;

        return new EqualsBuilder()
                .append(isValid(), that.isValid())
                .append(getTransactionId(), that.getTransactionId())
                .append(getUri(), that.getUri())
                .append(getHash(), that.getHash())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getTransactionId())
                .append(getUri())
                .append(getHash())
                .append(isValid())
                .toHashCode();
    }
}
