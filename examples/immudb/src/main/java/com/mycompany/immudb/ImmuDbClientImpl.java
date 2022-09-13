package com.mycompany.immudb;

import java.util.List;

import io.codenotary.immudb4j.Entry;
import io.codenotary.immudb4j.ImmuClient;
import io.codenotary.immudb4j.exceptions.CorruptedDataException;
import io.codenotary.immudb4j.exceptions.VerificationException;

/**
 * Implementation of the ImmuDbClient.
 */
public class ImmuDbClientImpl implements ImmuDbClient {
    // immuClient used to interact with the DB
    private final ImmuClient immuClient;

    /**
     * Constructor.
     * @param immuClient the immuClient
     */
    public ImmuDbClientImpl(ImmuClient immuClient) {
        this.immuClient = immuClient;
    }

    /**
     * Put a value in the DB
     * @param key the key
     * @param value the value, stored as a byte array
     * @throws CorruptedDataException if the data is corrupted
     */
    @Override
    public void putValue(String key, String value) throws CorruptedDataException {
        immuClient.set(key, value.getBytes());
    }

    /**
     * Get a value from the DB by key
     * @param key the key
     * @return the value, stored as a byte array
     * @throws VerificationException if the data is corrupted
     */
    @Override
    public String getValue(String key) throws VerificationException {
        Entry entry = immuClient.verifiedGet(key);

        if(entry != null){
            byte[] value = entry.getValue();
            return new String(value);
        }
        return null;
    }

    /**
     * Get all values from the DB for a list of keys
     * @param keyList the list of keys
     * @return the list of values, stored as a byte array
     */
    @Override
    public List<Entry> getAllValues(List<String> keyList) {
        return immuClient.getAll(keyList);
    }

    /**
     * Get the history of values for a key
     * @param key the key
     * @param limit the limit of values to return
     * @return the list of values, stored as a byte array
     */
    @Override
    public List<Entry> getHistoryValues(String key, int limit) {
        return immuClient.history(key, 10, 0, false);
    }

    /**
     * Scan the DB for keys that start with a prefix
     * @param prefix the prefix
     * @param limit the limit of values to return
     * @return the list of values, stored as a byte array
     */
    @Override
    public List<Entry> scanKeys(String prefix, int limit) {
        return immuClient.scan(prefix, limit, false);
    }
}
