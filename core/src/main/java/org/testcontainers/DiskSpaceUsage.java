package org.testcontainers;

import java.util.Optional;

class DiskSpaceUsage {
    Optional<Long> availableMB = Optional.empty();
    Optional<Integer> usedPercent = Optional.empty();

    static DiskSpaceUsage parseAvailableDiskSpace(String dfOutput) {
        DiskSpaceUsage df = new DiskSpaceUsage();
        String[] lines = dfOutput.split("\n");
        for (String line : lines) {
            String[] fields = line.split("\\s+");
            if (fields.length > 5 && fields[5].equals("/")) {
                long availableKB = Long.valueOf(fields[3]);
                df.availableMB = Optional.of(availableKB / 1024L);
                df.usedPercent = Optional.of(Integer.valueOf(fields[4].replace("%", "")));
                break;
            }
        }
        return df;
    }
}
