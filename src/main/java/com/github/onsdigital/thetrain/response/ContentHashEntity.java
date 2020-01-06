package com.github.onsdigital.thetrain.response;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ContentHashEntity {

    private String transactionId;
    private String uri;
    private String hash;

    public ContentHashEntity(String transactionId, String uri, String hash) {
        this.transactionId = transactionId;
        this.uri = uri;
        this.hash = hash;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        ContentHashEntity that = (ContentHashEntity) o;

        return new EqualsBuilder()
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
                .toHashCode();
    }
}
