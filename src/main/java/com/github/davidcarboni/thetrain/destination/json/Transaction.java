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

    Set<Uri> uris = new HashSet<>();
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
     * @return An unmodifiable set of the URIs in this transaction.
     */
    public Set<Uri> uris() {
        return Collections.unmodifiableSet(uris);
    }

    /**
     * @param uri The URI to add to the set of URIs.
     */
    public void addUri(Uri uri) {
        synchronized (this) {
            Set<Uri> uris = new HashSet<>(this.uris);
            uris.add(uri);
            this.uris = uris;
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

    @Override
    public String toString() {
        return id + " (" + uris.size() + " URIs)";
    }
}
