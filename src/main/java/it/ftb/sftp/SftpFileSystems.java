package it.ftb.sftp;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;

public final class SftpFileSystems {

    private SftpFileSystems() {
    }

    public static SftpFileSystem<? extends SftpPath> rooted(Path root) {
        return new RootedFileSystem(root.getFileSystem(), root.toAbsolutePath().normalize());
    }

    public static abstract class AbstractSftpFileSystem<P extends AbstractSftpPath<P>> implements SftpFileSystem<P> {

        protected final FileSystem fs;

        public AbstractSftpFileSystem(FileSystem fs) {
            this.fs = fs;
        }

        protected abstract P wrap(Path path);

        @Override
        public <A extends BasicFileAttributes> A readAttributes(P path, Class<A> type, LinkOption... options) throws IOException {
            return Files.readAttributes(path.path, type, options);
        }

        @Override
        public boolean isSymbolicLink(P path) {
            return Files.isSymbolicLink(path.path);
        }

        @Override
        public boolean isHidden(P path) throws IOException {
            return Files.isHidden(path.path);
        }

        @Override
        public boolean isDirectory(P path) {
            return Files.isDirectory(path.path);
        }

        @Override
        public boolean exists(P path) {
            return Files.exists(path.path);
        }

        @Override
        public DirectoryStream<P> newDirectoryStream(P path) throws IOException {
            DirectoryStream<Path> ds = Files.newDirectoryStream(path.path);
            return new DirectoryStream<P>() {
                @Override
                public Iterator<P> iterator() {
                    return Iterators.transform(ds.iterator(), AbstractSftpFileSystem.this::wrap);
                }

                @Override
                public void close() throws IOException {
                    ds.close();
                }
            };
        }

        @Override
        public SeekableByteChannel newByteChannel(P path, ImmutableSet<StandardOpenOption> options) throws IOException {
            return fs.provider().newByteChannel(path.path, options);
        }

        @Override
        public boolean isSameFile(P path1, P path2) {
            try {
                return Files.isSameFile(path1.path, path2.path);
            } catch (IOException e) {
                throw new IllegalStateException(e); // TODO, gestire meglio
            }
        }
    }

    public static abstract class AbstractSftpPath<P extends AbstractSftpPath<P>> implements SftpPath<P> {

        protected final AbstractSftpFileSystem<P> fs;
        protected final Path path;

        AbstractSftpPath(AbstractSftpFileSystem<P> fs, Path path) {
            Preconditions.checkNotNull(fs);
            Preconditions.checkNotNull(path);
            this.fs = fs;
            this.path = path;
        }

        @Override
        public P resolve(P other) {
            return fs.wrap(path.resolve(other.path));
        }

        @Override
        public P resolve(String other) {
            return fs.wrap(path.resolve(other));
        }

        @Override
        public P normalize() {
            return fs.wrap(path.normalize());
        }

        @Override
        public P toRealPath(LinkOption... options) throws IOException {
            return fs.wrap(path.toRealPath(options));
        }

        @Override
        public P getParent() {
            Path parent = path.getParent();
            return parent == null ? null : fs.wrap(parent);
        }

        @Override
        @Nonnull
        public String getFileName() {
            if (null != path.getFileName()) {
                return path.getFileName().toString();
            }
            return path.toString(); // TODO: verify that root element is properly identified
        }

        @Override
        public String toString() {
            return path.toString();
        }
    }

    private static final class RootedFileSystem extends AbstractSftpFileSystem<RootedPath> {

        private final RootedPath root;

        public RootedFileSystem(FileSystem fs, Path root) {
            super(fs);
            this.root = new RootedPath(this, root);
        }

        @Nonnull
        @Override
        public RootedPath getRoot() {
            return root;
        }

        @Nonnull
        @Override
        public RootedPath getHome() {
            return root;
        }

        @Override
        protected RootedPath wrap(Path path) {
            if (!path.toAbsolutePath().normalize().startsWith(root.path)) {
                throw new IllegalArgumentException("Path out of boundary");
            }
            return new RootedPath(this, path);
        }
    }

    private static final class RootedPath extends AbstractSftpPath<RootedPath> {

        public RootedPath(RootedFileSystem fs, Path path) {
            super(fs, path);
        }

        @Override
        public RootedPath normalize() {
            return isRoot() ? ((RootedFileSystem)fs).root : super.normalize();
        }

        @Override
        public RootedPath getParent() {
            return (isRoot() ? null : super.getParent());
        }

        @Override
        public RootedPath toRealPath(LinkOption... options) throws IOException {
            return super.toRealPath(options);
        }

        private boolean isRoot() {
            try {
                return Files.isSameFile(((RootedFileSystem)fs).root.path, path);
            } catch (IOException e) {
                throw new IllegalStateException("Unexpected...", e); // TODO: Si puo' evitare?
            }
        }

        @Nonnull
        @Override
        public String getFileName() {
            return isRoot() ? "" : super.getFileName();
        }

        @Override
        public String toString() {
            return ((RootedFileSystem)fs).root.path.relativize(path.toAbsolutePath()).toString();
        }
    }
}
