package it.ftb.sftp;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.LinkOption;

public interface SftpPath<P extends SftpPath<P>> {

    P resolve(P other);

    P resolve(String other);

    P normalize();

    P toRealPath(LinkOption... options) throws IOException;

    /**
     * Returns the parent path, or {@code null} if this path is a root element.
     *
     * @return The parent path
     */
    @Nullable
    P getParent();

    /**
     * Returns the name of the last element of this path.
     * Differently from java.nio.Path, the root elements must have a name
     *
     * @return The file name
     */
    @Nonnull
    String getFileName();

    static <P extends SftpPath<P>> String toString(SftpFileSystem<P> fs, P path) {
        StringBuilder sb = new StringBuilder();
        toString(fs, path, sb);
        return sb.toString();
    }

    static <P extends SftpPath<P>> void toString(SftpFileSystem<P> fs, P path, StringBuilder target) {
        P parent = path.getParent();
        if (parent != null) {
            toString(fs, parent, target);
            target.append('/');
            target.append(path.getFileName());
        } else if (fs.isSameFile(fs.getRoot(), path)) {
            target.append('/');
        } else {
            target.append(path.getFileName());
        }
    }

    static <P extends SftpPath<P>> P parse(SftpFileSystem<P> fs, String path) {
        boolean absolute = path.charAt(0) == '/';
        P ret = absolute ? fs.getRoot() : fs.getHome();
        int prevSlash = absolute ? 0 : -1;
        int slashIdx;
        while (-1 != (slashIdx = path.indexOf('/', prevSlash + 1))) {
            if (prevSlash + 1 != slashIdx) {
                ret = ret.resolve(path.substring(prevSlash + 1, slashIdx));
            }
            prevSlash = slashIdx;
        }
        if (prevSlash + 1 != path.length()) {
            ret = ret.resolve(path.substring(prevSlash + 1));
        }
        return ret;
    }
}
