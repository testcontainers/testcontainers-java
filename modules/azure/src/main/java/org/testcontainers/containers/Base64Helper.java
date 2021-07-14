package org.testcontainers.containers;

/**
 * Taken from jakarta xml bind DatatypeConverter#parseBase64Binary
 *
 * @author Onur Kagan Ozcan
 */
final class Base64Helper {

    private static final byte PADDING = 127;

    private static final byte[] decodeMap = initDecodeMap();

    private static byte[] initDecodeMap() {
        byte[] map = new byte[128];
        int i;
        for (i = 0; i < 128; i++) {
            map[i] = -1;
        }
        for (i = 'A'; i <= 'Z'; i++) {
            map[i] = (byte) (i - 'A');
        }
        for (i = 'a'; i <= 'z'; i++) {
            map[i] = (byte) (i - 'a' + 26);
        }
        for (i = '0'; i <= '9'; i++) {
            map[i] = (byte) (i - '0' + 52);
        }
        map['+'] = 62;
        map['/'] = 63;
        map['='] = PADDING;
        return map;
    }

    public static byte[] parseBase64Binary(String text) {
        final int buflen = guessLength(text);
        final byte[] out = new byte[buflen];
        int o = 0;
        //
        final int len = text.length();
        int i;
        //
        final byte[] quadruplet = new byte[4];
        int q = 0;
        for (i = 0; i < len; i++) {
            char ch = text.charAt(i);
            byte v = decodeMap[ch];
            if (v != -1) {
                quadruplet[q++] = v;
            }
            if (q == 4) {
                out[o++] = (byte) ((quadruplet[0] << 2) | (quadruplet[1] >> 4));
                if (quadruplet[2] != PADDING) {
                    out[o++] = (byte) ((quadruplet[1] << 4) | (quadruplet[2] >> 2));
                }
                if (quadruplet[3] != PADDING) {
                    out[o++] = (byte) ((quadruplet[2] << 6) | (quadruplet[3]));
                }
                q = 0;
            }
        }
        if (buflen == o) {
            return out;
        }
        byte[] nb = new byte[o];
        System.arraycopy(out, 0, nb, 0, o);
        return nb;
    }

    private static int guessLength(String text) {
        final int len = text.length();
        int j = len - 1;
        for (; j >= 0; j--) {
            byte code = decodeMap[text.charAt(j)];
            if (code == PADDING) {
                continue;
            }
            if (code == -1) {
                return text.length() / 4 * 3;
            }
            break;
        }
        j++;
        int padSize = len - j;
        if (padSize > 2) {
            return text.length() / 4 * 3;
        }
        return text.length() / 4 * 3 - padSize;
    }
}
