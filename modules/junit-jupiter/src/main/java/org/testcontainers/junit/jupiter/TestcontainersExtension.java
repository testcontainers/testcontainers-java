package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

class TestcontainersExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback {

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {

    }

    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {

    }

    @Override
    public void afterEach(final ExtensionContext context) throws Exception {

    }

    @Override
    public void afterAll(final ExtensionContext context) throws Exception {

    }

}
