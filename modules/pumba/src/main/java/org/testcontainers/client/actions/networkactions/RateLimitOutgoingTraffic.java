package org.testcontainers.client.actions.networkactions;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.testcontainers.client.commandparts.PumbaCommandPart;

/**
 * Created by novy on 15.01.17.
 */

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class RateLimitOutgoingTraffic implements NetworkSubCommands.NetworkSubCommand {
    private RateExpression rate = new RateExpression(100, RateUnit.KILOBITS_PER_SECOND);
    private SizeExpression packetOverhead = SizeExpression.ZERO;
    private SizeExpression cellSize = SizeExpression.ZERO;
    private SizeExpression cellOverhead = SizeExpression.ZERO;

    public RateLimitOutgoingTraffic to(long value, RateUnit unit) {
        this.rate = new RateExpression(value, unit);
        return this;
    }

    public RateLimitOutgoingTraffic withPacketOverhead(long value, SizeUnit unit) {
        this.packetOverhead = new SizeExpression(value, unit);
        return this;
    }

    public RateLimitOutgoingTraffic withCellSize(long value, SizeUnit unit) {
        this.cellSize = new SizeExpression(value, unit);
        return this;
    }

    public RateLimitOutgoingTraffic withCellOverhead(long value, SizeUnit unit) {
        this.cellOverhead = new SizeExpression(value, unit);
        return this;
    }

    private PumbaCommandPart rateCommandPart() {
        return () -> "rate";
    }

    private PumbaCommandPart rateValuePart() {
        return () -> "--rate " + rate.evaluate();
    }

    private PumbaCommandPart packetOverheadPart() {
        return () -> "--packetoverhead " + packetOverhead.toBytes();
    }

    private PumbaCommandPart cellSizePart() {
        return () -> "--cellsize " + cellSize.toBytes();
    }

    private PumbaCommandPart cellOverheadPart() {
        return () -> "--celloverhead " + cellOverhead.toBytes();
    }

    @Override
    public String evaluate() {
        return rateCommandPart()
                .append(rateValuePart())
                .append(packetOverheadPart())
                .append(cellSizePart())
                .append(cellOverheadPart())
                .evaluate();
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class RateExpression {
        private final long value;
        private final RateUnit unit;

        private String evaluate() {
            return value + unit.abbreviation();
        }
    }

    public enum RateUnit {
        KILOBITS_PER_SECOND {
            @Override
            String abbreviation() {
                return "kbit";
            }
        },
        MEGABITS_PER_SECOND {
            @Override
            String abbreviation() {
                return "mbit";
            }
        };

        abstract String abbreviation();
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class SizeExpression {
        private final long value;
        private final SizeUnit unit;

        private static final SizeExpression ZERO = new SizeExpression(0, SizeUnit.BYTES);

        private long toBytes() {
            return value * unit.toBytesMultiplier();
        }
    }

    public enum SizeUnit {
        BYTES {
            @Override
            long toBytesMultiplier() {
                return 1;
            }
        },
        KILOBYTES {
            @Override
            long toBytesMultiplier() {
                return 1000;
            }
        };

        abstract long toBytesMultiplier();
    }
}
