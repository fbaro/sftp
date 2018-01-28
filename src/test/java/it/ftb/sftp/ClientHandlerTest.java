package it.ftb.sftp;

import it.ftb.sftp.packet.PacketType;
import it.ftb.sftp.packet.SshFxpInit;
import it.ftb.sftp.packet.SshFxpVersion;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Pipe;
import java.nio.file.FileSystem;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ClientHandlerTest {

    @Test(timeout = 1000L)
    public void testDownloadFile() throws IOException, InterruptedException {
        Pipe clientWritePipe = Pipe.open();
        Pipe clientReadPipe = Pipe.open();
        ByteChannel clientChannel = new ByteChannel() {
            @Override
            public int read(ByteBuffer dst) throws IOException {
                return clientWritePipe.source().read(dst);
            }

            @Override
            public int write(ByteBuffer src) throws IOException {
                return clientReadPipe.sink().write(src);
            }

            @Override
            public boolean isOpen() {
                return clientWritePipe.source().isOpen() && clientReadPipe.sink().isOpen();
            }

            @Override
            public void close() throws IOException {
                clientWritePipe.source().close();
                clientReadPipe.sink().close();
            }
        };

        SftpFileSystem<?> fs = mock(SftpFileSystem.class);
        try (ClientHandler<?> handler = ClientHandler.start(clientChannel, fs)) {
            PacketEncoder writeEncoder = new PacketEncoder(clientWritePipe.sink());
            ChannelDecoder readDecoder = new ChannelDecoder(clientReadPipe.source());
            writeEncoder.write(new SshFxpInit(6));
            int len = readDecoder.readInt();
            PacketDecoder pd = new PacketDecoder(readDecoder, len);
            assertEquals(PacketType.SSH_FXP_VERSION.getCode(), pd.readByte());
            SshFxpVersion fxpVersion = SshFxpVersion.FACTORY.read(pd);
            assertEquals(6, fxpVersion.getuVersion());
        }
    }
}
