package it.ftb.sftp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.UnsignedInts;
import it.ftb.sftp.network.Bytes;
import it.ftb.sftp.packet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class ClientHandler implements AutoCloseable {

    private final Logger log = LoggerFactory.getLogger(ClientHandler.class);
    private final ByteChannel clientChannel;
    private final FileSystem fileSystem;
    private Reader reader;
    private Writer writer;

    private ClientHandler(ByteChannel clientChannel, FileSystem fileSystem) {
        this.clientChannel = clientChannel;
        this.fileSystem = fileSystem;
    }

    private void start() {
        reader = new Reader();
        writer = new Writer();
        reader.start();
        writer.start();
    }

    public static ClientHandler start(ByteChannel clientChannel, FileSystem fileSystem) {
        ClientHandler ret = new ClientHandler(clientChannel, fileSystem);
        ret.start();
        return ret;
    }

    @Override
    public void close() throws IOException, InterruptedException {
        reader.interrupt();
        writer.interrupt();
        reader.join();
        writer.join();
        clientChannel.close();
    }

    private final class Reader extends Thread implements VoidPacketVisitor<Void> {

        private final ChannelDecoder decoder = new ChannelDecoder(clientChannel);
        private final Map<Integer, SeekableByteChannel> openFiles = new HashMap<>();
        private int handlesCount = 0;

        @Override
        public void run() {
            try {
                int clientVersion = nextRequest().visit(null, new PacketVisitor<Void, Integer>() {
                    @Override
                    public Integer visit(AbstractPacket packet, Void parameter) {
                        throw new ProtocolException("First packet from client should be SSH_FXP_INIT");
                    }

                    @Override
                    public Integer visit(SshFxpInit packet, Void parameter) {
                        return packet.getuVersion();
                    }
                });
                if (clientVersion < 6) {
                    throw new ProtocolException("Unsupported protocol version " + clientVersion);
                }
                // TODO: Version negotiation
                writer.send(new SshFxpVersion(6, ImmutableList.of()));
                while (true) {
                    nextRequest().visit(null, this);
                }
            } catch (IOException | RuntimeIOException e) {
                log.debug("I/O exception", e);
            } catch (ProtocolException e) {
                log.debug("Protocol exception", e);
            } catch (RuntimeException e) {
                log.error("Unexpected exception", e);
            } finally {
                try {
                    clientChannel.close();
                } catch (IOException ignored) {
                }
            }
        }

        private AbstractPacket nextRequest() throws IOException {
            do {
                int packetLength = decoder.readOptInt().orElseThrow(ClosedChannelException::new);
                PacketDecoder pd = new PacketDecoder(decoder, packetLength);
                int packetCode = pd.readByte() & 0xff;
                PacketType packetType = PacketType.fromCode(packetCode);
                if (packetType != null) {
                    return packetType.getPacketFactory().read(pd);
                }
                log.debug("Received unsupported packet code: {}", packetCode);
                pd.skipRemaining();
            } while (true);
        }

        @Override
        public void visit(AbstractPacket packet, Void parameter) {
            log.debug("Received unexpected packet: {}", packet);
        }

        @Override
        public void visit(SshFxpOpen packet, Void parameter) {
            Path fsPath = fileSystem.getPath(packet.getFilename());
            try {
                SeekableByteChannel fileChannel = fileSystem.provider().newByteChannel(fsPath, ImmutableSet.of(StandardOpenOption.READ));
                int handle = ++handlesCount;
                openFiles.put(handle, fileChannel);
                writer.send(new SshFxpHandle(packet.getuRequestId(), Bytes.from(handle)));
            } catch (FileNotFoundException e) {
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_NO_SUCH_FILE, "File not found", "en"));
            } catch (IOException e) {
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_FAILURE, e.getMessage(), "en"));
            }
        }

        @Override
        public void visit(SshFxpClose packet, Void parameter) {
            SeekableByteChannel channel = openFiles.remove(packet.getHandle().asInt());
            if (channel == null) {
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_INVALID_HANDLE, "Handle not found", "en"));
                return;
            }
            try {
                channel.close();
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_OK, "", ""));
            } catch (IOException e) {
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_FAILURE, e.getMessage(), "en"));
            }
        }

        @Override
        public void visit(SshFxpRead packet, Void parameter) {
            SeekableByteChannel channel = openFiles.remove(packet.getHandle().asInt());
            if (channel == null) {
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_INVALID_HANDLE, "Handle not found", "en"));
                return;
            }
            try {
                channel.position(packet.getuOffset());
                int length = UnsignedInts.min(packet.getuLength(), 0x10000);
                ByteBuffer data = ByteBuffer.allocate(length);
                int numRead = 0;
                while (data.hasRemaining() && -1 != (numRead = channel.read(data))) {
                    // Keep reading
                }
                data.flip();
                writer.send(new SshFxpData(packet.getuRequestId(), Bytes.hold(data), numRead == -1));
            } catch (IOException e) {
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_FAILURE, e.getMessage(), "en"));
            }
        }
    }

    private static final AbstractPacket POISON = new SshFxpVersion(0, ImmutableList.of());

    private final class Writer extends Thread {

        private final BlockingQueue<AbstractPacket> toWrite = new ArrayBlockingQueue<>(16);
        private final PacketEncoder encoder = new PacketEncoder(clientChannel);

        @Override
        public void run() {
            try {
                AbstractPacket packet;
                while (POISON != (packet = toWrite.take())) {
                    encoder.write(packet);
                }
            } catch (RuntimeIOException ex) {
                log.debug("Error writing to client", ex);
            } catch (InterruptedException ignored) {
                // Ok
            } catch (RuntimeException ex) {
                log.error("Unexpected error", ex);
            }
            // TODO: cleanup
        }

        public void send(AbstractPacket packet) {
            try {
                toWrite.put(packet);
            } catch (InterruptedException e) {
                throw new RuntimeInterruptedException(e);
            }
        }
    }
}
