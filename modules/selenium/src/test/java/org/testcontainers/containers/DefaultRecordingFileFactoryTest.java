package org.testcontainers.containers;

import lombok.Value;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;

@RunWith(Parameterized.class)
@Value
public class DefaultRecordingFileFactoryTest {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("YYYYMMdd-HHmmss");

    private final DefaultRecordingFileFactory factory = new DefaultRecordingFileFactory();

    private final String methodName;

    private final String prefix;

    private final boolean success;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> args = new ArrayList<>();
        args.add(new Object[] { "testMethod1", "FAILED", Boolean.FALSE });
        args.add(new Object[] { "testMethod2", "PASSED", Boolean.TRUE });
        return args;
    }
}
