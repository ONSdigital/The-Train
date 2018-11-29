package com.github.davidcarboni.thetrain.json;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.thetrain.helpers.DateConverter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Details of a single transaction, including any files transferred and any errors encountered.
 * NB a {@link Transaction} is the unit of synchronization, so methods that manipulate the collections in this class synchronize on <code>this</code>.
 */
public class Transaction {

    private final transient Object uriInfosLock = new Object();
    private final transient Object uriDeletesLock = new Object();
    private final transient Object errorsLock = new Object();

    public static final String STARTED = "started";
    public static final String PUBLISHING = "publishing";
    public static final String COMMIT_FAILED = "commit failed";
    public static final String COMMITTED = "committed";
    public static final String ROLLED_BACK = "rolled back";
    public static final String ROLLBACK_FAILED = "rollback failed";

    // Whilst an ID collision is technically possible it's a
    // theoretical rather than a practical consideration.
    private String id = Random.id();
    private String status = STARTED;
    private String startDate = DateConverter.toString(new Date());
    private String endDate;

    private Set<UriInfo> uriInfos = new HashSet<>();
    private Set<UriInfo> uriDeletes = new HashSet<>();
    private List<String> errors = new ArrayList<>();

    /**
     * The actual files on disk in this transaction.
     * This might differ slightly from {@link #uriInfos}
     * if there is an issue, so useful to have a direct view of these.
     */
    public Map<String, List<String>> files;

    /**
     * @return The transaction {@link #id}.
     */
    public String id() {
        return id;
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

    public String getStatus() {
        return status;
    }

    /**
     * @return An unmodifiable set of the URIs in this transaction.
     */
    public Set<UriInfo> uris() {
        synchronized (uriInfosLock) {
            return Collections.unmodifiableSet(uriInfos);
        }
    }

    /**
     * @return An unmodifiable set of the URIs to delete in this transaction.
     */
    public Set<UriInfo> urisToDelete() {
        synchronized (uriDeletesLock) {
            return Collections.unmodifiableSet(uriDeletes);
        }
    }

    /**
     * @param uriInfo The URI to add to the set of URIs.
     */
    public void addUri(UriInfo uriInfo) {
        synchronized (uriInfosLock) {
            uriInfos.add(uriInfo);
            status = PUBLISHING;
        }
    }

    public void addUris(List<UriInfo> infos) {
        synchronized (uriInfosLock) {
            uriInfos.addAll(infos);
            status = PUBLISHING;
        }
    }

    /**
     * Add a delete command to the transaction.
     *
     * @param uriInfo
     */
    public void addUriDelete(UriInfo uriInfo) {
        synchronized (uriDeletesLock) {
            uriDeletes.add(uriInfo);
        }
    }

    public void addUriDeletes(List<UriInfo> deletes) {
        synchronized (uriDeletesLock) {
            uriDeletes.addAll(deletes);
        }
    }

    /**
     * @return If the status of the transaction is {@value #STARTED} or {@value #PUBLISHING}, true, otherwise false.
     */
    public boolean isOpen() {
        return StringUtils.equals(STARTED, status) || StringUtils.equals(PUBLISHING, status);
    }

    /**
     * Checks for errors in this transaction.
     *
     * @return If {@link #errors} contains anything, or if any {@link UriInfo#error error} field in {@link #uriInfos} is not blank, true.
     */
    public boolean hasErrors() {
        synchronized (errorsLock) {
            boolean result = errors.size() > 0;
            for (UriInfo uriInfo : uriInfos) {
                result |= StringUtils.isNotBlank(uriInfo.error());
            }
            return result;
        }
    }

    /**
     * @return An unmodifiable set of the URIs in this transaction.
     */
    public List<String> errors() {
        synchronized (errorsLock) {
            return Collections.unmodifiableList(errors);
        }
    }

    /**
     * @param error An error debug to be added to this transaction.
     */
    public void addError(String error) {
        synchronized (errorsLock) {
            errors.add(error);
        }
    }

    public void commit(boolean success) {
        endDate = DateConverter.toString(new Date());
        if (success) {
            status = COMMITTED;
        } else {
            status = COMMIT_FAILED;
        }
    }

    public void rollback(boolean success) {
        endDate = DateConverter.toString(new Date());
        if (success) {
            status = ROLLED_BACK;
        } else {
            status = ROLLBACK_FAILED;
        }
    }

    @Override
    public String toString() {
        synchronized (uriInfosLock) {
            return id + " (" + uriInfos.size() + " URIs)";
        }
    }
}
