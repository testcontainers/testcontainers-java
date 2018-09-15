package org.testcontainers;

import java.util.Optional;

class DiskSpaceUsage {
    private Long availableMB;
    private Integer usedPercent;

    Optional<Long> getAvailableMB() {
        return Optional.of(availableMB);
    }

    Optional<Integer> getUsedPercent() {
        return Optional.of(usedPercent);
    }

    static DiskSpaceUsage parseAvailableDiskSpace(String dfOutput) {
        DiskSpaceUsage df = new DiskSpaceUsage();
        String[] lines = dfOutput.split("\n");
        for (String line : lines) {
            String[] fields = line.split("\\s+");
            if (fields.length > 5 && fields[5].equals("/")) {
                long availableKB = Long.valueOf(fields[3]);
                df.availableMB = availableKB / 1024L;
                df.usedPercent = Integer.valueOf(fields[4].replace("%", ""));
                break;
            }
        }
        return df;
    }
}
