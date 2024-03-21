package org.testcontainers.containers;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;

@Slf4j
class MongoDBContainerDef extends BaseContainerDef {

    private static final int CONTAINER_EXIT_CODE_OK = 0;

    private static final int AWAIT_INIT_REPLICA_SET_ATTEMPTS = 60;

    private static final String STARTER_SCRIPT = "/testcontainers_start.sh";

    private static final int MONGODB_INTERNAL_PORT = 27017;

    private boolean shardingEnabled;

    MongoDBContainerDef() {
        addExposedTcpPort(MONGODB_INTERNAL_PORT);
        setCommand("--replSet", "docker-rs");
        setWaitStrategy(Wait.forLogMessage("(?i).*waiting for connections.*", 1));
    }

    void withSharding() {
        this.shardingEnabled = true;
        setCommand("-c", "while [ ! -f " + STARTER_SCRIPT + " ]; do sleep 0.1; done; " + STARTER_SCRIPT);
        setWaitStrategy(Wait.forLogMessage("(?i).*mongos ready.*", 1));
        setEntrypoint("sh");
    }

    @Override
    protected StartedMongoDBContainer toStarted(ContainerState containerState) {
        return new MongoDBStarted(containerState);
    }

    class MongoDBStarted extends BaseContainerDef.Started implements StartedMongoDBContainer, ContainerLifecycleHooks {

        public MongoDBStarted(ContainerState containerState) {
            super(containerState);
        }

        @Override
        public String getConnectionString() {
            return String.format(
                "mongodb://%s:%d",
                getHost(),
                getMappedPort(MongoDBContainerDef.MONGODB_INTERNAL_PORT)
            );
        }

        @Override
        public void containerIsStarting(boolean reused) {
            if (shardingEnabled) {
                copyFileToContainer(MountableFile.forClasspathResource("/sharding.sh", 0777), STARTER_SCRIPT);
            }
        }

        @Override
        public void containerIsStarted(boolean reused) {
            if (!shardingEnabled) {
                initReplicaSet(reused);
            }
        }

        @SneakyThrows(value = { IOException.class, InterruptedException.class })
        private void initReplicaSet(boolean reused) {
            if (reused && isReplicationSetAlreadyInitialized()) {
                log.debug("Replica set already initialized.");
            } else {
                log.debug("Initializing a single node node replica set...");
                final Container.ExecResult execResultInitRs = execInContainer(buildMongoEvalCommand("rs.initiate();"));
                log.debug(execResultInitRs.getStdout());
                checkMongoNodeExitCode(execResultInitRs);

                log.debug(
                    "Awaiting for a single node replica set initialization up to {} attempts",
                    AWAIT_INIT_REPLICA_SET_ATTEMPTS
                );
                final Container.ExecResult execResultWaitForMaster = execInContainer(
                    buildMongoEvalCommand(buildMongoWaitCommand())
                );
                log.debug(execResultWaitForMaster.getStdout());

                checkMongoNodeExitCodeAfterWaiting(execResultWaitForMaster);
            }
        }

        @SneakyThrows
        private boolean isReplicationSetAlreadyInitialized() {
            // since we are creating a replica set with one node, this node must be primary (state = 1)
            final Container.ExecResult execCheckRsInit = execInContainer(
                buildMongoEvalCommand("if(db.adminCommand({replSetGetStatus: 1})['myState'] != 1) quit(900)")
            );
            return execCheckRsInit.getExitCode() == CONTAINER_EXIT_CODE_OK;
        }

        private void checkMongoNodeExitCodeAfterWaiting(final Container.ExecResult execResultWaitForMaster) {
            if (execResultWaitForMaster.getExitCode() != CONTAINER_EXIT_CODE_OK) {
                final String errorMessage = String.format(
                    "A single node replica set was not initialized in a set timeout: %d attempts",
                    AWAIT_INIT_REPLICA_SET_ATTEMPTS
                );
                log.error(errorMessage);
                throw new ReplicaSetInitializationException(errorMessage);
            }
        }

        private String[] buildMongoEvalCommand(final String command) {
            return new String[] {
                "sh",
                "-c",
                "mongosh mongo --eval \"" + command + "\"  || mongo --eval \"" + command + "\"",
            };
        }

        private void checkMongoNodeExitCode(final Container.ExecResult execResult) {
            if (execResult.getExitCode() != CONTAINER_EXIT_CODE_OK) {
                final String errorMessage = String.format("An error occurred: %s", execResult.getStdout());
                log.error(errorMessage);
                throw new ReplicaSetInitializationException(errorMessage);
            }
        }

        private String buildMongoWaitCommand() {
            return String.format(
                "var attempt = 0; " +
                "while" +
                "(%s) " +
                "{ " +
                "if (attempt > %d) {quit(1);} " +
                "print('%s ' + attempt); sleep(100);  attempt++; " +
                " }",
                "db.runCommand( { isMaster: 1 } ).ismaster==false",
                AWAIT_INIT_REPLICA_SET_ATTEMPTS,
                "An attempt to await for a single node replica set initialization:"
            );
        }

        class ReplicaSetInitializationException extends RuntimeException {

            ReplicaSetInitializationException(final String errorMessage) {
                super(errorMessage);
            }
        }
    }
}
