package com.github.davidcarboni.thetrain.destination.json;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by david on 31/07/2015.
 */
public class Transaction implements Cloneable {
    public String id;
    public String startDate;
    public Set<Timing> uris = new HashSet<>();
    public List<String> errors = new ArrayList<>();


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
