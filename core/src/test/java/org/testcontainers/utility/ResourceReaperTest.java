package org.testcontainers.utility;

import org.junit.Test;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class ResourceReaperTest {

    @Test
    public void toQueryUrlEncodesFilters() {
        String query = ResourceReaper.toQuery(Arrays.asList(
            new SimpleEntry<>("key1!", "value2?"),
            new SimpleEntry<>("key2#", "value2%")
        ));

        assertEquals("key1%21=value2%3F&key2%23=value2%25", query);
    }
}
