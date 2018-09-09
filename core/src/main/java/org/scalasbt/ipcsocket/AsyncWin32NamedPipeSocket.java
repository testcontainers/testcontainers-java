package org.scalasbt.ipcsocket;

import com.sun.jna.platform.win32.WinNT;

import java.io.IOException;

/**
 * TODO remove when sbt/ipcsocket 1.0.1 is out
 * See https://github.com/sbt/ipcsocket/pull/6
 */
public class AsyncWin32NamedPipeSocket extends Win32NamedPipeSocket {

    private static final CloseCallback EMPTY_CALLBACK = handle -> {
    };

    public AsyncWin32NamedPipeSocket(String pipeName) throws IOException {
        super(
            Win32NamedPipeLibrary.INSTANCE.CreateFile(
                pipeName,
                WinNT.GENERIC_READ | WinNT.GENERIC_WRITE,
                0,     // no sharing
                null,  // default security attributes
                WinNT.OPEN_EXISTING,
                WinNT.FILE_FLAG_OVERLAPPED,     // default attributes
                null
            ),
            EMPTY_CALLBACK,
            DEFAULT_REQUIRE_STRICT_LENGTH
        );
    }
}
