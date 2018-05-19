package org.testcontainers.client.actions.networkactions;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testcontainers.client.commandparts.PumbaCommandPart;
import org.testcontainers.client.commandparts.TimeExpression;

import java.time.Duration;
import java.util.Optional;

/**
 * Created by novy on 14.01.17.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NetworkActions {

    public static NetworkActionWithSubCommand networkAction() {
        return new NetworkActionWithSubCommand();
    }

    public static class NetworkActionWithSubCommand {

        private TimeExpression duration = TimeExpression.of(Duration.ofMinutes(1));
        private String networkInterface = "eth0";
        private String targetIP;

        public NetworkActionWithSubCommand lastingFor(Duration duration) {
            this.duration = TimeExpression.of(duration);
            return this;
        }

        public NetworkActionWithSubCommand onNetworkInterface(String networkInterface) {
            this.networkInterface = networkInterface;
            return this;
        }

        public NetworkActionWithSubCommand onTrafficTo(String targetIP) {
            this.targetIP = targetIP;
            return this;
        }

        public NetworkAction executeSubCommand(NetworkSubCommands.NetworkSubCommand subCommand) {
            return () -> netemPart()
                .append(durationPart())
                .append(interfacePart())
                .append(trafficFilterPart())
                .append(subCommand)
                .evaluate();
        }

        private PumbaCommandPart netemPart() {
            return () -> "netem --tc-image gaiadocker/iproute2:3.3";
        }

        private PumbaCommandPart durationPart() {
            return () -> "--duration " + duration.evaluate();
        }

        private PumbaCommandPart interfacePart() {
            return () -> "--interface " + networkInterface;
        }

        private PumbaCommandPart trafficFilterPart() {
            return () -> Optional.ofNullable(targetIP).map(t -> "--target " + t).orElse("");
        }
    }
}
