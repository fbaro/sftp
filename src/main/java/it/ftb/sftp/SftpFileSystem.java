package it.ftb.sftp;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

public interface SftpFileSystem {
    SftpPath getPath(String path);

    <A extends BasicFileAttributes> A readAttributes(
            SftpPath path, Class<A> type, LinkOption... options) throws IOException;

    boolean isSymbolicLink(SftpPath path);

    boolean isHidden(SftpPath path) throws IOException;

    boolean isDirectory(SftpPath path);

    boolean exists(SftpPath path);

    DirectoryStream<SftpPath> newDirectoryStream(SftpPath path) throws IOException;

    SeekableByteChannel newByteChannel(SftpPath path, ImmutableSet<StandardOpenOption> options) throws IOException;
}
