package com.github.davidcarboni.thetrain.destination.json;

import com.github.davidcarboni.cryptolite.Random;
import org.apache.commons.lang3.StringUtils;

import java.util.*;


/**
 * Created by david on 31/07/2015.
 */
public class Transaction implements Cloneable {

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
        synchronized (uris) {
            uris.add(uri);
        }
    }

    /**
     * @param error An error message to be added to this transaction.
     */
    public void addError(String error) {
        synchronized (errors) {
            errors.add(error);
        }
    }

    /**
     * @return A clone of this instance.
     */
    public Transaction clone() {
        try {
            Transaction transaction = (Transaction) super.clone();
            transaction.uris = new HashSet<>();
            for (Uri uri : uris) {
                transaction.uris.add(uri.clone());
            }
            return transaction;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int hashCode() {
        int result = 0;
        if (id != null) {
            result = id.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null &&
                obj.getClass().equals(this.getClass()) &&
                StringUtils.equals(id, ((Uri) obj).uri);
    }

}
