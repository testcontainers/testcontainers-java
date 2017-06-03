package org.testcontainers.client.actions.networkactions;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testcontainers.client.commandparts.PumbaCommandPart;

/**
 * Created by novy on 14.01.17.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NetworkSubCommands {

    public static DelayOutgoingPackets delayOutgoingPackets() {
        return new DelayOutgoingPackets();
    }

    public static RateLimitOutgoingTraffic rateLimitOutgoingTraffic() {
        return new RateLimitOutgoingTraffic();
    }

    public static DroppingPacketsModels lossOutgoingPackets() {
        return new DroppingPacketsModels();
    }


    interface NetworkSubCommand extends PumbaCommandPart {
    }
}
