package org.testcontainers.examples;

import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasspathResource("org/testcontainers/examples/is_search_possible.feature")
public class CucumberTest {
}
