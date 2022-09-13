package com.mycompany.immudb;

import java.util.List;
import io.codenotary.immudb4j.Entry;
import io.codenotary.immudb4j.exceptions.CorruptedDataException;
import io.codenotary.immudb4j.exceptions.VerificationException;

/**
 * Interface for the ImmuDbClient.
 */
public interface ImmuDbClient {
    /**
     * Put a value in the DB
     * @param key the key
     * @param value the value, stored as a byte array
     * @throws CorruptedDataException if the data is corrupted
     */
    void putValue(String key, String value) throws CorruptedDataException;
    /**
     * Get a value from the DB by key
     * @param key the key
     * @return the value, stored as a byte array
     * @throws VerificationException if the data is corrupted
     */
    String getValue(String key) throws VerificationException;
    /**
     * Get all values from the DB for a list of keys
     * @param keyList the list of keys
     * @return the list of values, stored as a byte array
     */
    List<Entry> getAllValues(List<String> keyList);
    /**
     * Get the history of values for a key
     * @param key the key
     * @param limit the limit of values to return
     * @return the list of values, stored as a byte array
     */
    List<Entry> getHistoryValues(String key, int limit);
    /**
     * Scan the DB for keys that start with a prefix
     * @param prefix the prefix
     * @param limit the limit of values to return
     * @return the list of values, stored as a byte array
     */
    List<Entry> scanKeys(String prefix, int limit);
}
