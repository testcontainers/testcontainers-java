package org.testcontainers.containers.output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("Duplicates")
public abstract class BaseConsumer<SELF extends BaseConsumer<SELF>> implements Consumer<OutputFrame> {
    private StringBuilder logString = new StringBuilder();
    private OutputFrame brokenFrame;
    private boolean removeColorCodes = true;

    public SELF withRemoveAnsiCodes(boolean removeAnsiCodes) {
        this.removeColorCodes = removeAnsiCodes;
        return (SELF) this;
    }

    public abstract void process(OutputFrame outputFrame);

    @Override
    public final synchronized void accept(OutputFrame outputFrame) {
        if (outputFrame != null) {
            String utf8String = outputFrame.getUtf8String();
            byte[] bytes = outputFrame.getBytes();

            if (utf8String != null && !utf8String.isEmpty()) {
                // Merging the strings by bytes to solve the problem breaking non-latin unicode symbols.
                if (brokenFrame != null) {
                    bytes = merge(brokenFrame.getBytes(), bytes);
                    utf8String = new String(bytes);
                    brokenFrame = null;
                }
                // Logger chunks can break the string in middle of multibyte unicode character.
                // Backup the bytes to reconstruct proper char sequence with bytes from next line.
                if (Character.getType(utf8String.charAt(utf8String.length() - 1)) == Character.OTHER_SYMBOL) {
                    brokenFrame = new OutputFrame(outputFrame.getType(), bytes);
                    return;
                }

                if (removeColorCodes) {
                    utf8String = utf8String.replaceAll("\u001B\\[[0-9;]+m", "");
                }

                // Reformat strings to normalize enters.
                List<String> lines = new ArrayList<>(Arrays.asList(utf8String.split("((\\r?\\n)|(\\r))")));
                if (lines.isEmpty()) {
                    process(new OutputFrame(outputFrame.getType(), "".getBytes()));
                    return;
                }
                if (utf8String.startsWith("\n") || utf8String.startsWith("\r")) {
                    lines.add(0, "");
                }
                if (utf8String.endsWith("\n") || utf8String.endsWith("\r")) {
                    lines.add("");
                }
                for (int i = 0; i < lines.size() - 1; i++) {
                    String line = lines.get(i);
                    if (i == 0 && logString.length() > 0) {
                        line = logString.toString() + line;
                        logString.setLength(0);
                    }
                    process(new OutputFrame(outputFrame.getType(), line.getBytes()));
                }
                logString.append(lines.get(lines.size() - 1));
            }
        }
    }

    private byte[] merge(byte[] str1, byte[] str2) {
        byte[] mergedString = new byte[str1.length + str2.length];
        System.arraycopy(str1, 0, mergedString, 0, str1.length);
        System.arraycopy(str2, 0, mergedString, str1.length, str2.length);
        return mergedString;
    }
}
