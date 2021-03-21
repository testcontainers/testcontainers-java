package org.testcontainers.ext;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.regex.Pattern;

public final class FilenameComparator
implements Comparator<String> {

    private static final Pattern NUMBERS =
        Pattern.compile("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
    private static final Pattern FILE_ENDING =
        Pattern.compile("(?<=.*)(?=\\..*)");

    @Override
    public final int compare(String o1, String o2) {
        if (o1 == null || o2 == null) {
            return o1 == null ? o2 == null ? 0 : -1 : 1;
        }

        String[] name1 = FILE_ENDING.split(o1);
        String[] name2 = FILE_ENDING.split(o2);

        String[] split1 = NUMBERS.split(name1[0]);
        String[] split2 = NUMBERS.split(name2[0]);
        int length = Math.min(split1.length, split2.length);

        // Looping over the individual segments
        for (int i = 0; i < length; i++) {
            char c1 = split1[i].charAt(0);
            char c2 = split2[i].charAt(0);
            int cmp = 0;

            if (c1 >= '0' && c1 <= '9' && c2 >= 0 && c2 <= '9') {
                cmp = new BigInteger(split1[i]).compareTo(
                    new BigInteger(split2[i]));
            }

            if (cmp == 0) {
                cmp = split1[i].compareTo(split2[i]);
            }

            if (cmp != 0) {
                return cmp;
            }
        }

        int cmp = split1.length - split2.length;
        if (cmp != 0) {
            return cmp;
        }

        cmp = name1.length - name2.length;
        if (cmp != 0) {
            return cmp;
        }

        return name1[1].compareTo(name2[1]);
    }
}
