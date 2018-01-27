package it.ftb.sftp;

import java.io.IOException;

public interface SftpPath {
    boolean isAbsolute();

    SftpPath resolve(SftpPath other);

    SftpPath normalize();

    SftpPath toRealPath() throws IOException;

    String getFileName();

    String toString();
}

