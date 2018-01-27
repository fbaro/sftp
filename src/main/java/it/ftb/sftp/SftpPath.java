package it.ftb.sftp;

import java.io.IOException;
import java.nio.file.LinkOption;

public interface SftpPath<P extends SftpPath<P>> {
    boolean isAbsolute();

    P resolve(P other);

    P normalize();

    P toRealPath(LinkOption... options) throws IOException;

    P getParent() throws IOException;

    String getFileName();
}
