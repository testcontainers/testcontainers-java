package org.testcontainers.containers;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;

/**
 * @author Eugeny Karpov
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ReusableContainerConfiguration {

    private final String containerName;
    private final boolean isEnabled;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String containerName;
        private Boolean isEnabled = true;

        public Builder withContainerName(String containerName) {
            this.containerName = containerName;
            return this;
        }

        public Builder isEnabled(boolean isEnabled) {
            this.isEnabled = isEnabled;
            return this;
        }

        public ReusableContainerConfiguration build() {
            if (StringUtils.isBlank(containerName)) {
                throw new IllegalArgumentException("Container name must be specified with REUSABLE mode");
            }
            return new ReusableContainerConfiguration(containerName, isEnabled);
        }
    }
}
