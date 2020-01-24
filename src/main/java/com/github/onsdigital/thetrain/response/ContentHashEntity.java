package com.github.onsdigital.thetrain.response;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * POJO containing the hash value of the requested content.
 */
public class ContentHashEntity {

    private String transactionId;
    private String uri;
    private String hash;

    /**
     * @param transactionId the ID of the {@link com.github.onsdigital.thetrain.json.Transaction} the content belongs
     *                      too.
     * @param uri           the URI of the content being requested.
     * @param hash          the hash value of the content.
     */
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
