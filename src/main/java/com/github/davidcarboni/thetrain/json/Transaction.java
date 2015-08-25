package com.github.davidcarboni.thetrain.json;

import com.github.davidcarboni.cryptolite.KeyWrapper;
import com.github.davidcarboni.cryptolite.Keys;
import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.thetrain.helpers.DateConverter;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.util.*;


/**
 * Details of a single transaction, including any files transferred and any errors encountered.
 * <p/>
 * NB a {@link Transaction} is the unit of synchronization, so methods that manipulate the collections in this class synchronize on <code>this</code>.
 */
public class Transaction {

    // Whilst an ID collision is technically possible it's a
    // theoretical rather than a practical consideration.
    String id = Random.id();
    String startDate = DateConverter.toString(new Date());
    String endDate;
    String wrappedKey;
    String salt;
    transient SecretKey key;

    Set<UriInfo> uriInfos = new HashSet<>();
    List<String> errors = new ArrayList<>();

    /**
     * The actual files on disk in this transaction.
     * This might differ slightly from {@link #uriInfos}
     * if there is an issue, so useful to have a direct view of these.
     */
    public Map<String, List<String>> files;


    /**
     * Sets encryption-related fields for this transaction.
     *
     * @param password If this is not blank, encryption-related fields will be initialised.
     */
    public void enableEncryption(String password) {

        if (StringUtils.isNotBlank(password)) {
            if (StringUtils.isBlank(wrappedKey)) {
                // Set up a key
                key = Keys.newSecretKey();
                salt = Random.salt();
                wrappedKey = new KeyWrapper(password, salt).wrapSecretKey(key);
            } else {
                // Unwrap the existing key
                key = new KeyWrapper(password, salt).unwrapSecretKey(wrappedKey);
            }
        }
    }

    /**
     * @return The transaction {@link #id}.
     */
    public String id() {
        return id;
    }

    /**
     * @return The encryption key for this transaction, if encryption is enabled, otherwise null.
     */
    public SecretKey key() {
        return key;
    }

    /**
     * @return The transaction {@link #startDate}.
     */
    public String startDate() {
        return startDate;
    }

    /**
     * @return The transaction {@link #endDate}.
     */
    public String endDate() {
        return endDate;
    }

    /**
     * @return An unmodifiable set of the URIs in this transaction.
     */
    public Set<UriInfo> uris() {
        return Collections.unmodifiableSet(uriInfos);
    }

    /**
     * @param uriInfo The URI to add to the set of URIs.
     */
    public void addUri(UriInfo uriInfo) {
        synchronized (this) {
            Set<UriInfo> uriInfos = new HashSet<>(this.uriInfos);
            uriInfos.add(uriInfo);
            this.uriInfos = uriInfos;
        }
    }

    /**
     * Checks for errors in this transaction.
     *
     * @return If {@link #errors} contains anything, or if any {@link UriInfo#error error} field in {@link #uriInfos} is not blank, true.
     */
    public boolean hasErrors() {
        boolean result = errors.size() > 0;
        for (UriInfo uriInfo : uriInfos) {
            result |= StringUtils.isNotBlank(uriInfo.error());
        }
        return result;
    }

    /**
     * @return An unmodifiable set of the URIs in this transaction.
     */
    public List<String> errors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * @param error An error message to be added to this transaction.
     */
    public void addError(String error) {
        synchronized (this) {
            List<String> errors = new ArrayList<>(this.errors);
            errors.add(error);
            this.errors = errors;
        }
    }

    public void end() {
        endDate = DateConverter.toString(new Date());
    }

    @Override
    public String toString() {
        return id + " (" + uriInfos.size() + " URIs)";
    }


}
