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

     Set<Timing> uris = new HashSet<>();
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
    public Set<Timing> uris() {
        return Collections.unmodifiableSet(uris);
    }

    /**
     * @param timing The URI to add to the set of URIs.
     */
    public void addUri(Timing timing) {
        synchronized (uris) {
            uris.add(timing);
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
            for (Timing timing : uris) {
                transaction.uris.add(timing.clone());
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
                StringUtils.equals(id, ((Timing) obj).uri);
    }

}
