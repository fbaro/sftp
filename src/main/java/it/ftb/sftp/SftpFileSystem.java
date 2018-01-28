package it.ftb.sftp;

import com.google.common.collect.ImmutableSet;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Interface to interact with the file system.
 * It is a slight variation on the FileSystem interface, supporting a single root element
 * @param <P> The SftpPath instances this SftpFileSystem returns
 */
public interface SftpFileSystem<P extends SftpPath<P>> {

    @Nonnull
    P getRoot();

    @Nonnull
    P getHome();

    <A extends BasicFileAttributes> A readAttributes(
            P path, Class<A> type, LinkOption... options) throws IOException;

    boolean isSymbolicLink(P path);

    boolean isHidden(P path) throws IOException;

    boolean isDirectory(P path);

    boolean exists(P path);

    DirectoryStream<P> newDirectoryStream(P path) throws IOException;

    SeekableByteChannel newByteChannel(P path, ImmutableSet<StandardOpenOption> options) throws IOException;

    boolean isSameFile(P path1, P path2);
}

