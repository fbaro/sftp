package it.ftb.sftp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.UnsignedInts;
import it.ftb.sftp.network.Bytes;
import it.ftb.sftp.packet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
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
        private final Map<Integer, FileData> openFiles = new HashMap<>();               // TODO: Limitare il numero di entries
        private final Map<Integer, DirectoryData> openDirectories = new HashMap<>();    // TODO: Limitare il numero di entries
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
                    AbstractPacket packet = nextRequest();
                    log.debug("Received: {}", packet);
                    try {
                        packet.visit(null, this);
                    } catch (RuntimeException ex) {
                        if (packet instanceof RequestPacket) {
                            writer.send(new SshFxpStatus(((RequestPacket) packet).getuRequestId(), ErrorCode.SSH_FX_FAILURE,
                                    ex.getMessage(), "en"));
                        }
                    }
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
                if (packetType == null) {
                    log.warn("Received unsupported packet code {}, {} bytes long", packetCode, packetLength);
                    pd.skipRemaining();
                } else {
                    log.trace("Received packet {}, {} bytes long", packetType, packetLength);
                    return packetType.getPacketFactory().read(pd);
                }
            } while (true);
        }

        @Override
        public void visit(AbstractPacket packet, Void parameter) {
            log.debug("Received unexpected packet: {}", packet);
        }

        @Override
        public void visit(SshFxpLstat packet, Void parameter) {
            Path path = fileSystem.getPath(packet.getPath());
            try {
                Attrs attrs = Attrs.create(path, packet.getuFlags(), LinkOption.NOFOLLOW_LINKS);
                writer.send(new SshFxpAttrs(packet.getuRequestId(), attrs));
            } catch (IOException e) {
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_FAILURE, e.getMessage(), "en"));
            }
        }

        @Override
        public void visit(SshFxpStat packet, Void parameter) {
            Path path = fileSystem.getPath(packet.getPath());
            try {
                Attrs attrs = Attrs.create(path, packet.getuFlags());
                writer.send(new SshFxpAttrs(packet.getuRequestId(), attrs));
            } catch (IOException e) {
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_FAILURE, e.getMessage(), "en"));
            }
        }

        @Override
        public void visit(SshFxpFstat packet, Void parameter) {
            int handle = packet.getHandle().asInt();
            Path path;
            if (openFiles.containsKey(handle)) {
                path = openFiles.get(handle).path;
            } else if (openDirectories.containsKey(handle)) {
                path = openDirectories.get(handle).path;
            } else {
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_INVALID_HANDLE, "Handle not found", "en"));
                return;
            }
            try {
                Attrs attrs = Attrs.create(path, packet.getuFlags());
                writer.send(new SshFxpAttrs(packet.getuRequestId(), attrs));
            } catch (IOException e) {
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_FAILURE, e.getMessage(), "en"));
            }
        }

        @Override
        public void visit(SshFxpRealpath packet, Void parameter) {
            Path path = fileSystem.getPath(packet.getOriginalPath());
            for (String cp : packet.getComposePath()) {
                Path pComponent = fileSystem.getPath(cp);
                path = pComponent.isAbsolute() ? pComponent : path.resolve(pComponent);
            }
            path = path.normalize();
            switch (packet.getControlByte()) {
                case SSH_FXP_REALPATH_NO_CHECK:
                    writer.send(new SshFxpName(packet.getuRequestId(),
                            ImmutableList.of(path.toString()),
                            ImmutableList.of(Attrs.EMPTY),
                            Optional.of(true)));
                    break;
                case SSH_FXP_REALPATH_STAT_IF:
                    try {
                        path = path.toRealPath();
                        Attrs attrs = Attrs.create(path);
                        writer.send(new SshFxpName(packet.getuRequestId(),
                                ImmutableList.of(path.toString()),
                                ImmutableList.of(attrs),
                                Optional.of(true)));
                    } catch (IOException e) {
                        writer.send(new SshFxpName(packet.getuRequestId(),
                                ImmutableList.of(path.toString()),
                                ImmutableList.of(Attrs.EMPTY),
                                Optional.of(true)));
                    }
                    break;

                case SSH_FXP_REALPATH_STAT_ALWAYS:
                    try {
                        path = path.toRealPath();
                        Attrs attrs = Attrs.create(path);
                        writer.send(new SshFxpName(packet.getuRequestId(),
                                ImmutableList.of(path.toString()),
                                ImmutableList.of(attrs),
                                Optional.of(true)));
                    } catch (IOException e) {
                        writer.send(new SshFxpStatus(packet.getuRequestId(),
                                ErrorCode.SSH_FX_NO_SUCH_FILE,
                                e.getMessage(),
                                "en"));
                    }
                    break;
                default:
                    throw new IllegalStateException("Unknown control byte " + packet.getControlByte());
            }
        }

        @Override
        public void visit(SshFxpOpenDir packet, Void parameter) {
            Path path = fileSystem.getPath(packet.getPath());
            if (Files.exists(path) && !Files.isDirectory(path)) {
                writer.send(new SshFxpStatus(packet.getuRequestId(),
                        ErrorCode.SSH_FX_NOT_A_DIRECTORY,
                        null, null));
            } else {
                try {
                    DirectoryStream<Path> dirStream = Files.newDirectoryStream(path);
                    int handle = ++handlesCount;
                    openDirectories.put(handle, new DirectoryData(path, dirStream));
                    writer.send(new SshFxpHandle(packet.getuRequestId(), Bytes.from(handle)));
                } catch (FileNotFoundException e) {
                    writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_NO_SUCH_FILE, "File not found", "en"));
                } catch (IOException e) {
                    writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_FAILURE, e.getMessage(), "en"));
                }
            }
        }

        @Override
        public void visit(SshFxpReadDir packet, Void parameter) {
            DirectoryData dirStream = openDirectories.get(packet.getHandle().asInt());
            if (dirStream == null) {
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_INVALID_HANDLE, "Handle not found", "en"));
            } else {
                ImmutableList.Builder<String> names = new ImmutableList.Builder<>();
                ImmutableList.Builder<Attrs> attributes = new ImmutableList.Builder<>();
                for (int i = 0; i < 16 && dirStream.iterator.hasNext(); i++) {
                    Path path = dirStream.iterator.next();
                    names.add(path.getFileName().toString());
                    try {
                        attributes.add(Attrs.create(path));
                    } catch (IOException e) {
                        attributes.add(Attrs.EMPTY);
                    }
                }
                writer.send(new SshFxpName(packet.getuRequestId(), names.build(), attributes.build(), !dirStream.iterator.hasNext()));
            }
        }

        @Override
        public void visit(SshFxpOpen packet, Void parameter) {
            Path fsPath = fileSystem.getPath(packet.getFilename());
            try {
                SeekableByteChannel fileChannel = fileSystem.provider().newByteChannel(fsPath, ImmutableSet.of(StandardOpenOption.READ));
                int handle = ++handlesCount;
                openFiles.put(handle, new FileData(fileChannel, fsPath));
                writer.send(new SshFxpHandle(packet.getuRequestId(), Bytes.from(handle)));
            } catch (FileNotFoundException e) {
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_NO_SUCH_FILE, "File not found", "en"));
            } catch (IOException e) {
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_FAILURE, e.getMessage(), "en"));
            }
        }

        @Override
        public void visit(SshFxpClose packet, Void parameter) {
            int handle = packet.getHandle().asInt();
            Closeable closeable = openFiles.remove(handle);
            if (closeable == null) {
                closeable = openDirectories.remove(handle);
            }
            if (closeable == null) {
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_INVALID_HANDLE, "Handle not found", "en"));
            } else {
                try {
                    closeable.close();
                    writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_OK, "", ""));
                } catch (IOException e) {
                    writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_FAILURE, e.getMessage(), "en"));
                }
            }
        }

        @Override
        public void visit(SshFxpRead packet, Void parameter) {
            FileData fileData = openFiles.get(packet.getHandle().asInt());
            if (fileData == null) {
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_INVALID_HANDLE, "Handle not found", "en"));
                return;
            }
            try {
                fileData.channel.position(packet.getuOffset());
                int length = UnsignedInts.min(packet.getuLength(), 0x10000);
                ByteBuffer data = ByteBuffer.allocate(length);
                int numRead = 0;
                while (data.hasRemaining() && -1 != (numRead = fileData.channel.read(data))) {
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
                    log.debug("Sent: {}", packet);
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

    private static final class DirectoryData implements Closeable {
        public final DirectoryStream<Path> stream;
        public final Iterator<Path> iterator;
        public final Path path;

        public DirectoryData(Path path, DirectoryStream<Path> stream) {
            this.path = path;
            this.stream = stream;
            this.iterator = stream.iterator();
        }

        @Override
        public void close() throws IOException {
            stream.close();
        }
    }

    private static final class FileData implements Closeable {
        public final SeekableByteChannel channel;
        public final Path path;

        public FileData(SeekableByteChannel channel, Path path) {
            this.channel = channel;
            this.path = path;
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }
    }
}

