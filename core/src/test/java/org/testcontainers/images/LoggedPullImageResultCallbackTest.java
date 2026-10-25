package org.testcontainers.images;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.model.PullResponseItem;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThatCode;

class LoggedPullImageResultCallbackTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggedPullImageResultCallbackTest.class);

    @Test
    void testZeroDurationPullDoesNotThrowArithmeticException() throws Exception {
        LoggedPullImageResultCallback callback = new LoggedPullImageResultCallback(LOGGER);
        callback.onStart(null);

        ObjectMapper mapper = new ObjectMapper();
        PullResponseItem item = mapper.readValue(
            "{\"status\":\"Download complete\",\"id\":\"layer1\"}",
            PullResponseItem.class
        );
        callback.onNext(item);

        assertThatCode(callback::onComplete).doesNotThrowAnyException();
    }
}
