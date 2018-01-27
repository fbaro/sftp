package it.ftb.sftp;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;

public final class SftpFileSystems {

    private SftpFileSystems() {
    }

    public static SftpFileSystem<? extends SftpPath> rooted(Path root) {
        return new RootedFileSystem(root.getFileSystem(), root.toAbsolutePath());
    }

    public static abstract class AbstractSftpFileSystem<P extends AbstractSftpPath<P>> implements SftpFileSystem<P> {

        protected final FileSystem fs;

        public AbstractSftpFileSystem(FileSystem fs) {
            this.fs = fs;
        }

        protected abstract P wrap(Path path);

        @SuppressWarnings("StaticPseudoFunctionalStyleMethod")
        @Override
        public Iterable<P> getRootDirectories() {
            return Iterables.transform(fs.getRootDirectories(), this::wrap);
        }

        @Override
        public P getPath(String path) {
            return wrap(fs.getPath(path));
        }

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
    }

    public static abstract class AbstractSftpPath<P extends AbstractSftpPath<P>> implements SftpPath<P> {

        protected final AbstractSftpFileSystem<P> fs;
        protected final Path path;

        AbstractSftpPath(AbstractSftpFileSystem<P> fs, Path path) {
            this.fs = fs;
            this.path = path;
        }

        @Override
        public P resolve(P other) {
            return fs.wrap(path.resolve(other.path));
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
        public P getParent() throws IOException {
            return fs.wrap(path.getParent());
        }

        @Override
        public boolean isAbsolute() {
            return path.isAbsolute();
        }

        @Override
        public String getFileName() {
            return path.getFileName().toString();
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }

    private static final class RootedFileSystem extends AbstractSftpFileSystem<RootedPath> {

        private final RootedPath root;

        public RootedFileSystem(FileSystem fs, Path root) {
            super(fs);
            this.root = new RootedPath(this, root);
        }

        @Override
        protected RootedPath wrap(Path path) {
            return new RootedPath(this, path);
        }
    }

    private static final class RootedPath extends AbstractSftpPath<RootedPath> {

        public RootedPath(RootedFileSystem fs, Path path) {
            super(fs, path);
        }

        @Override
        public RootedPath getParent() throws IOException {
            if (Files.isSameFile(((RootedFileSystem)fs).root.path, path)) {
                return null;
            }
            return super.getParent();
        }

        @Override
        public String toString() {
            return ((RootedFileSystem)fs).root.path.relativize(path.toAbsolutePath()).toString();
        }
    }
}
