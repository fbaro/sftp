package it.ftb.sftp;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;

public final class SftpFileSystems {

    private SftpFileSystems() {
    }

    public static SftpFileSystem wrap(FileSystem fs) {
        return new JavaFileSystem(fs);
    }

    public static class JavaFileSystem implements SftpFileSystem {

        protected final FileSystem fs;

        public JavaFileSystem(FileSystem fs) {
            this.fs = fs;
        }

        @Override
        public SftpPath getPath(String path) {
            return new JavaPath(fs.getPath(path));
        }

        @Override
        public <A extends BasicFileAttributes> A readAttributes(SftpPath path, Class<A> type, LinkOption... options) throws IOException {
            return Files.readAttributes(unwrap(path), type, options);
        }

        @Override
        public boolean isSymbolicLink(SftpPath path) {
            return Files.isSymbolicLink(unwrap(path));
        }

        @Override
        public boolean isHidden(SftpPath path) throws IOException {
            return Files.isHidden(unwrap(path));
        }

        @Override
        public boolean isDirectory(SftpPath path) {
            return Files.isDirectory(unwrap(path));
        }

        @Override
        public boolean exists(SftpPath path) {
            return Files.exists(unwrap(path));
        }

        @Override
        public DirectoryStream<SftpPath> newDirectoryStream(SftpPath path) throws IOException {
            DirectoryStream<Path> ds = Files.newDirectoryStream(unwrap(path));
            return new DirectoryStream<SftpPath>() {
                @Override
                @Nonnull
                public Iterator<SftpPath> iterator() {
                    return Iterators.transform(ds.iterator(), JavaPath::new);
                }

                @Override
                public void close() throws IOException {
                    ds.close();
                }
            };
        }

        @Override
        public SeekableByteChannel newByteChannel(SftpPath path, ImmutableSet<StandardOpenOption> options) throws IOException {
            return fs.provider().newByteChannel(unwrap(path), options);
        }

        protected static Path unwrap(SftpPath path) {
            return ((JavaPath)path).path;
        }
    }

    public static class JavaPath implements SftpPath {

        protected final Path path;

        public JavaPath(Path path) {
            this.path = path;
        }

        @Override
        public boolean isAbsolute() {
            return path.isAbsolute();
        }

        @Override
        public SftpPath resolve(SftpPath other) {
            return new JavaPath(path.resolve(JavaFileSystem.unwrap(other)));
        }

        @Override
        public SftpPath normalize() {
            return new JavaPath(path.normalize());
        }

        @Override
        public SftpPath toRealPath() throws IOException {
            return new JavaPath(path.toRealPath());
        }

        @Override
        public String getFileName() {
            return path.getFileName().toString();
        }

        @Override
        public String toString() {
            return path.toString();
        }
    }
}
