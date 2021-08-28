package org.testcontainers.controller.intents;

public interface RemoveImageIntent {
    RemoveImageIntent withForce(boolean force);
    void perform();
}
