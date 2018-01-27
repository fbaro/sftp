package it.ftb.sftp;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

public interface SftpFileSystem<P> {

    Iterable<? extends P> getRootDirectories();

    P getPath(String path);

    <A extends BasicFileAttributes> A readAttributes(
            P path, Class<A> type, LinkOption... options) throws IOException;

    boolean isSymbolicLink(P path);

    boolean isHidden(P path) throws IOException;

    boolean isDirectory(P path);

    boolean exists(P path);

    DirectoryStream<P> newDirectoryStream(P path) throws IOException;

    SeekableByteChannel newByteChannel(P path, ImmutableSet<StandardOpenOption> options) throws IOException;
}
