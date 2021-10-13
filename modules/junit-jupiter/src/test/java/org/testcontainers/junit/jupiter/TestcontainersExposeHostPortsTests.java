package org.testcontainers.junit.jupiter;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.ReflectionUtils.HierarchyTraversalMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PortForwardingContainer;

@Testcontainers(exposeHostPorts = { 8080, 8081 })
public class TestcontainersExposeHostPortsTests {

    @Container
    GenericContainer<?> foo = new GenericContainer<>(JUnitJupiterTestImages.HTTPD_IMAGE);

    @Test
    public void testExposeHostPosts() throws IllegalArgumentException, IllegalAccessException {
        List<Field> fields = ReflectionUtils.findFields(PortForwardingContainer.class,
            field -> field.getName().equals("exposedPorts"),
            HierarchyTraversalMode.BOTTOM_UP);

        Field field = fields.get(0);
        field.setAccessible(true);

        Set<Entry<Integer, Integer>> exposedPorts = (Set<Entry<Integer, Integer>>) field.get(PortForwardingContainer.INSTANCE);

        assertThat(exposedPorts)
            .hasSize(2)
            .anyMatch(entry -> entry.getKey().equals(8080))
            .anyMatch(entry -> entry.getKey().equals(8081));
    }

}
