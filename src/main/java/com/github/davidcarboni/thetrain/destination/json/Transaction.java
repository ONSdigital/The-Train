package com.github.davidcarboni.thetrain.destination.json;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.thetrain.destination.helpers.DateConverter;

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

    Set<UriInfo> uriInfos = new HashSet<>();
    List<String> errors = new ArrayList<>();

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
