package it.ftb.sftp;

import org.junit.Test;

import java.nio.file.FileSystems;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class PathHandlerTest {

    @Test
    public void testRootPaths() {
        Path root = FileSystems.getDefault().getRootDirectories().iterator().next();
        assertEquals(root, root.getRoot());
        assertEquals(root, root.getRoot().getRoot());
        assertEquals(null, root.getParent());
        assertNull(root.getFileName());
    }
}