package org.testcontainers.containers;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Lazy variant of FileSystem Bind that defers calculation of values used to create
 * {@link com.github.dockerjava.api.model.Bind}
 */
public interface LazyFileSystemBind {
    String getHostPath();

    String getContainerPath();

    BindMode getMode();

    default SelinuxContext getSelinuxContext() {
        return SelinuxContext.NONE;
    }

    class Builder {
        private Supplier<String> hostPath;
        private Supplier<String> containerPath;
        private Supplier<BindMode> mode;
        private Supplier<SelinuxContext> selinuxContext = () -> SelinuxContext.NONE;

        public Builder withHostPath(String hostPath) {
            this.hostPath = () -> requireNonNull(hostPath, "hostPath cannot be null");
            return this;
        }

        public Builder withHostPath(Supplier<String> hostPath) {
            this.hostPath = requireNonNull(hostPath, "hostPath cannot be null");
            return this;
        }

        public Builder withContainerPath(String containerPath) {
            this.containerPath = () -> requireNonNull(containerPath, "containerPath cannot be null");
            return this;
        }

        public Builder withContainerPath(Supplier<String> containerPath) {
            this.containerPath = requireNonNull(containerPath, "containerPath cannot be null");
            return this;
        }

        public Builder withMode(BindMode mode) {
            this.mode = () -> requireNonNull(mode, "mode cannot be null");
            return this;
        }

        public Builder withMode(Supplier<BindMode> mode) {
            this.mode = requireNonNull(mode, "mode cannot be null");
            return this;
        }

        public Builder withSelinuxContext(SelinuxContext selinuxContext) {
            this.selinuxContext = () -> requireNonNull(selinuxContext, "mode cannot be null");
            return this;
        }

        public Builder withSelinuxContext(Supplier<SelinuxContext> selinuxContext) {
            this.selinuxContext = requireNonNull(selinuxContext, "selinuxContext cannot be null");
            return this;
        }

        public LazyFileSystemBind build() {
            requireNonNull(hostPath, "hostPath supplier cannot be null");
            requireNonNull(containerPath, "containerPath supplier cannot be null");
            requireNonNull(mode, "mode supplier cannot be null");
            requireNonNull(selinuxContext, "selinuxContext supplier cannot be null");

            return new LazyFileSystemBind() {
                @Override
                public String getHostPath() {
                    return requireNonNull(hostPath.get(), "hostPath lazy value cannot be null");
                }

                @Override
                public String getContainerPath() {
                    return requireNonNull(containerPath.get(), "containerPath lazy value cannot be null");
                }

                @Override
                public BindMode getMode() {
                    return requireNonNull(mode.get(), "mode lazy value cannot be null");
                }

                @Override
                public SelinuxContext getSelinuxContext() {
                    return requireNonNull(selinuxContext.get(), "selinuxContext lazy value cannot be null");
                }
            };
        }
    }
}
