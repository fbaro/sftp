package it.ftb.sftp;

import com.google.common.collect.ImmutableList;
import it.ftb.sftp.packet.AbstractPacket;
import it.ftb.sftp.packet.SshFxpInit;
import it.ftb.sftp.packet.SshFxpVersion;
import org.junit.Test;

import java.io.IOException;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultPacketProcessorTest {

    @Test(timeout = 1000L)
    public void testDownloadFile() throws IOException, InterruptedException {
        SftpFileSystem<?> fs = mock(SftpFileSystem.class);
        Consumer<AbstractPacket> output = mock(Consumer.class);
        DefaultPacketProcessor<?> dpp = new DefaultPacketProcessor<>(fs, output);
        dpp.visitInit(6, null);
        verify(output).accept(eq(new SshFxpVersion(6, ImmutableList.of())));
    }
}