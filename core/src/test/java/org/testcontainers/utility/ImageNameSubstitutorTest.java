package org.testcontainers.utility;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.testcontainers.utility.ImageNameSubstitutor.LogWrappedImageNameSubstitutor;

import static org.junit.Assert.assertTrue;

public class ImageNameSubstitutorTest {

    @Rule
    public MockTestcontainersConfigurationRule config = new MockTestcontainersConfigurationRule();

    @Test
    public void simpleConfigrationTest() {
        final ImageNameSubstitutor original = ImageNameSubstitutor.instance;
        ImageNameSubstitutor.instance = null;
        try {
            Mockito
                .doReturn(FakeImageSubstitutor.class.getCanonicalName())
                .when(TestcontainersConfiguration.getInstance())
                .getImageSubstitutorClassName();

            final ImageNameSubstitutor imageNameSubstitutor = ImageNameSubstitutor.instance();

            assertTrue(imageNameSubstitutor instanceof LogWrappedImageNameSubstitutor);
            assertTrue(((LogWrappedImageNameSubstitutor) imageNameSubstitutor).wrappedInstance instanceof FakeImageSubstitutor);
        } finally {
            ImageNameSubstitutor.instance = original;
        }
    }

}
