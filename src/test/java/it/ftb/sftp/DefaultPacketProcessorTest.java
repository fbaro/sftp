package it.ftb.sftp;

import com.google.common.collect.ImmutableList;
import it.ftb.sftp.packet.VoidPacketVisitor;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultPacketProcessorTest {

    @Test(timeout = 1000L)
    public void testDownloadFile() {
        SftpFileSystem<?> fs = mock(SftpFileSystem.class);
        VoidPacketVisitor output = mock(VoidPacketVisitor.class);
        DefaultPacketProcessor<?> dpp = new DefaultPacketProcessor<>(fs, output);
        dpp.visitInit(6);
        verify(output).visitVersion(6, ImmutableList.of());
    }
}