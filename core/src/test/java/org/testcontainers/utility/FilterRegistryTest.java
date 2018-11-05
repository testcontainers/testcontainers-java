package org.testcontainers.utility;

import org.junit.Test;
import org.testcontainers.utility.ResourceReaper.FilterRegistry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map.Entry;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FilterRegistryTest {

    private static final List<Entry<String, String>> FILTERS = asList(
        new SimpleEntry<>("key1!", "value2?"), new SimpleEntry<>("key2#", "value2%")
    );
    private static final String URL_ENCODED_FILTERS = "key1%21=value2%3F&key2%23=value2%25";
    private static final byte[] ACKNOWLEDGEMENT = FilterRegistry.ACKNOWLEDGMENT.getBytes();
    private static final byte[] NO_ACKNOWLEDGEMENT = "".getBytes();
    private static final String NEW_LINE = "\n";

    @Test
    public void registerReturnsTrueIfAcknowledgementIsReadFromInputStream() throws IOException {
        FilterRegistry registry = new FilterRegistry(inputStream(ACKNOWLEDGEMENT), anyOutputStream());

        boolean successful = registry.register(FILTERS);

        assertTrue(successful);
    }

    @Test
    public void registerReturnsFalseIfNoAcknowledgementIsReadFromInputStream() throws IOException {
        FilterRegistry registry = new FilterRegistry(inputStream(NO_ACKNOWLEDGEMENT), anyOutputStream());

        boolean successful = registry.register(FILTERS);

        assertFalse(successful);
    }

    @Test
    public void registerWritesUrlEncodedFiltersAndNewlineToOutputStream() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        FilterRegistry registry = new FilterRegistry(anyInputStream(), outputStream);

        registry.register(FILTERS);

        assertEquals(URL_ENCODED_FILTERS + NEW_LINE, new String(outputStream.toByteArray()));
    }

    private static InputStream inputStream(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

    private static InputStream anyInputStream() {
        return inputStream(ACKNOWLEDGEMENT);
    }

    private static OutputStream anyOutputStream() {
        return new ByteArrayOutputStream();
    }

}
