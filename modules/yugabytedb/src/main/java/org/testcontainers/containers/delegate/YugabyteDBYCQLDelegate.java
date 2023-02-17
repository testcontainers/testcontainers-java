package org.testcontainers.containers.delegate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.YugabyteDBYCQLContainer;
import org.testcontainers.ext.ScriptUtils.UncategorizedScriptException;

import java.util.Collection;

/**
 * Query execution delegate class for YCQL API to delegate init-script statements. This
 * invokes the in-built <code>ycqlsh</code> cli within the container to execute the
 * statements at one go. It is recommended to use frameworks such as liquibase to manage
 * this requirement. This functionality is kept to address the initialization requirements
 * from standalone services that can't leverage liquibase or something similar.
 *
 * @see YugabyteDBYCQLContainer
 */
@RequiredArgsConstructor
@Slf4j
public final class YugabyteDBYCQLDelegate extends AbstractYCQLDelegate {

    private static final String BIN_PATH = "/home/yugabyte/tserver/bin/ycqlsh";

    private final YugabyteDBYCQLContainer container;

    @Override
    public void execute(
        Collection<String> statements,
        String scriptPath,
        boolean continueOnError,
        boolean ignoreFailedDrops
    ) {
        try {
            ExecResult result = container.execInContainer(
                BIN_PATH,
                "-u",
                container.getUsername(),
                "-p",
                container.getPassword(),
                "-k",
                container.getKeyspace(),
                "-e",
                StringUtils.join(statements, ";")
            );
            if (result.getExitCode() != 0) {
                throw new RuntimeException(result.getStderr());
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            throw new UncategorizedScriptException(e.getMessage(), e);
        }
    }
}
