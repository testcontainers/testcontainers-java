package org.testcontainers.client.actions.containeractions;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testcontainers.client.commandparts.PumbaCommandPart;

/**
 * Created by novy on 16.01.17.
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class KillContainers implements ContainerAction {
    private KillSignal signal = KillSignal.SIGKILL;

    public KillContainers withSignal(KillSignal signal) {
        this.signal = signal;
        return this;
    }

    @Override
    public String evaluate() {
        return killCommandPart()
                .append(signalPart())
                .evaluate();
    }

    private PumbaCommandPart killCommandPart() {
        return () -> "kill";
    }

    private PumbaCommandPart signalPart() {
        return () -> "--signal " + signal.name();
    }

    public enum KillSignal {
        SIGHUP,
        SIGINT,
        SIGQUIT,
        SIGILL,
        SIGTRAP,
        SIGIOT,
        SIGBUS,
        SIGFPE,
        SIGKILL,
        SIGUSR1,
        SIGSEGV,
        SIGUSR2,
        SIGPIPE,
        SIGALRM,
        SIGTERM,
        SIGSTKFLT,
        SIGCHLD,
        SIGCONT,
        SIGSTOP,
        SIGTSTP,
        SIGTTIN,
        SIGTTOU,
        SIGURG,
        SIGXCPU,
        SIGXFSZ,
        SIGVTALRM,
        SIGPROF,
        SIGWINCH,
        SIGIO,
        SIGPWR;
    }
}
