package org.testcontainers.controller.intents;


public interface ExecCreateIntent {
    ExecCreateIntent withAttachStdout(boolean attachStdout);

    ExecCreateIntent withAttachStderr(boolean attachStderr);

    ExecCreateIntent withCmd(String... command);

    ExecCreateResult perform();
}
