package org.testcontainers.containers.traits;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.google.common.collect.ObjectArrays;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.utility.SelfReference;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@RequiredArgsConstructor
public class FileSystemBind<SELF extends Container<SELF>> implements Trait<SELF> {

    public interface Support<SELF extends Container<SELF>> extends SelfReference<SELF> {

        /**
         * Adds a file system binding. Consider using {@link #withFileSystemBind(String, String, BindMode)}
         * for building a container in a fluent style.
         *
         * @param hostPath the file system path on the host
         * @param containerPath the file system path inside the container
         * @param mode the bind mode
         */
        default void addFileSystemBind(String hostPath, String containerPath, BindMode mode) {
            self().with(new FileSystemBind<>(hostPath, containerPath, mode));
        }

        /**
         * Adds a file system binding.
         *
         * @param hostPath the file system path on the host
         * @param containerPath the file system path inside the container
         * @param mode the bind mode
         * @return this
         */
        default SELF withFileSystemBind(String hostPath, String containerPath, BindMode mode) {
            addFileSystemBind(hostPath, containerPath, mode);

            return self();
        }

        default List<Bind> getBinds() {
            Stream<FileSystemBind> traits = self().getTraits(FileSystemBind.class);
            return traits.map(FileSystemBind::getBind).collect(Collectors.toList());
        }

        default void setBinds(List<Bind> binds) {
            self().replaceTraits(FileSystemBind.class, binds.stream().map(FileSystemBind::new));
        }
    }

    protected final Bind bind;

    public FileSystemBind(String hostPath, String containerPath, BindMode mode) {
        this(new Bind(hostPath, new Volume(containerPath), mode.accessMode));
    }

    @Override
    public void configure(SELF container, CreateContainerCmd createContainerCmd) {
        Bind[] currentBinds = createContainerCmd.getBinds();
        createContainerCmd.withBinds(ObjectArrays.concat(currentBinds, bind));
    }
}
