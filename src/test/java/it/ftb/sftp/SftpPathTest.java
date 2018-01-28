package it.ftb.sftp;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SftpPathTest {

    @Test
    public void testToString() {
    }

    @Test
    @SuppressWarnings("unchecked")
    public <P extends SftpPath<P>> void testParseRoot() {
        SftpFileSystem<P> fs = mock(SftpFileSystem.class);
        P root = (P) mock(SftpPath.class);
        when(fs.getRoot()).thenReturn(root);
        assertSame(root, SftpPath.parse(fs, "/"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public <P extends SftpPath<P>> void testParseAbsolutePath() {
        SftpFileSystem<P> fs = mock(SftpFileSystem.class);
        P root = (P) mock(SftpPath.class);
        P test1 = (P) mock(SftpPath.class);
        P test2 = (P) mock(SftpPath.class);
        when(fs.getRoot()).thenReturn(root);
        when(root.resolve("test1")).thenReturn(test1);
        when(test1.resolve("test2")).thenReturn(test2);
        assertSame(test2, SftpPath.parse(fs, "/test1/test2"));
        assertSame(test2, SftpPath.parse(fs, "/test1/test2/"));
    }
}
