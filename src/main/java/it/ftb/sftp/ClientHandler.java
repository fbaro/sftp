package it.ftb.sftp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiConsumer;

public final class ClientHandler<P extends SftpPath<P>> implements AutoCloseable {

    @SuppressWarnings("OctalInteger")
    private static final Map<PosixFilePermission, Integer> POSIX_FILE_PERMISSION_MASK =
            ImmutableMap.<PosixFilePermission, Integer>builder()
                    .put(PosixFilePermission.OWNER_READ, 0000400)
                    .put(PosixFilePermission.OWNER_WRITE, 0000200)
                    .put(PosixFilePermission.OWNER_EXECUTE, 0000100)
                    .put(PosixFilePermission.GROUP_READ, 0000040)
                    .put(PosixFilePermission.GROUP_WRITE, 0000020)
                    .put(PosixFilePermission.GROUP_EXECUTE, 0000010)
                    .put(PosixFilePermission.OTHERS_READ, 0000004)
                    .put(PosixFilePermission.OTHERS_WRITE, 0000002)
                    .put(PosixFilePermission.OTHERS_EXECUTE, 0000001)
                    .build();
    private final Logger log = LoggerFactory.getLogger(ClientHandler.class);
    private final ByteChannel clientChannel;
    private final SftpFileSystem<P> fileSystem;
    private Reader reader;
    private Writer writer;

    private ClientHandler(ByteChannel clientChannel, SftpFileSystem<P> fileSystem) {
        this.clientChannel = clientChannel;
        this.fileSystem = fileSystem;
    }

    private Attrs createAttrs(P path) throws IOException {
        return createAttrs(path, 0xffffffff);
    }

    private Attrs createAttrs(P path, int uInterestedInFlags, LinkOption... linkOptions) throws IOException {
        try {
            return fileSystem.readAttributes(path, Attrs.class, linkOptions);
        } catch (UnsupportedOperationException ignored) {
            // I gave it a try, never mind
        }

        Attrs.Type type;
        BasicFileAttributes attributes;
        if (fileSystem.isSymbolicLink(path)) {
            type = Attrs.Type.SSH_FILEXFER_TYPE_SYMLINK;
            attributes = fileSystem.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } else {
            attributes = fileSystem.readAttributes(path, BasicFileAttributes.class, linkOptions);
            if (attributes.isDirectory()) {
                type = Attrs.Type.SSH_FILEXFER_TYPE_DIRECTORY;
            } else if (attributes.isRegularFile()) {
                type = Attrs.Type.SSH_FILEXFER_TYPE_REGULAR;
            } else if (attributes.isOther()) {
                type = Attrs.Type.SSH_FILEXFER_TYPE_SPECIAL;
            } else {
                type = Attrs.Type.SSH_FILEXFER_TYPE_UNKNOWN;
            }
        }
        Attrs.Builder builder = new Attrs.Builder(type, true);
        if (type == Attrs.Type.SSH_FILEXFER_TYPE_REGULAR) {
            builder.withSize(attributes.size());
        }
        setTime(attributes.lastModifiedTime(), builder::withMtime);
        setTime(attributes.lastAccessTime(), builder::withAtime);
        builder.withAttribute(Attrs.Attribute.SSH_FILEXFER_ATTR_FLAGS_HIDDEN, fileSystem.isHidden(path));
        if (Attrs.Validity.SSH_FILEXFER_ATTR_OWNERGROUP.isSet(uInterestedInFlags)
                || Attrs.Validity.SSH_FILEXFER_ATTR_PERMISSIONS.isSet(uInterestedInFlags)) {
            try {
                PosixFileAttributes pfa = fileSystem.readAttributes(path, PosixFileAttributes.class, linkOptions);
                builder.withOwnerGroup(pfa.owner().getName(), pfa.group().getName());
                int permissions = 0;
                for (PosixFilePermission p : pfa.permissions()) {
                    permissions |= POSIX_FILE_PERMISSION_MASK.get(p);
                }
                builder.withPermissions(permissions);
            } catch (UnsupportedOperationException ignored) {
                // Launched by Files.readAttributes, never mind
            }
        }
        if (Attrs.Validity.SSH_FILEXFER_ATTR_BITS.isSet(uInterestedInFlags)) {
            try {
                DosFileAttributes dfa = fileSystem.readAttributes(path, DosFileAttributes.class, linkOptions);
                builder.withAttribute(Attrs.Attribute.SSH_FILEXFER_ATTR_FLAGS_ARCHIVE, dfa.isArchive());
                builder.withAttribute(Attrs.Attribute.SSH_FILEXFER_ATTR_FLAGS_READONLY, dfa.isReadOnly());
                builder.withAttribute(Attrs.Attribute.SSH_FILEXFER_ATTR_FLAGS_SYSTEM, dfa.isSystem());
            } catch (UnsupportedOperationException ignored) {
                // Launched by Files.readAttributes, never mind
            }
        }
        return builder.build();
    }

    private static void setTime(FileTime fileTime, BiConsumer<Long, Integer> setter) {
        long epochSec = fileTime.toInstant().getEpochSecond();
        int epochNanosec = fileTime.toInstant().getNano();
        if (epochSec != 0) {
            setter.accept(epochSec, epochNanosec);
        }
    }

    private void start() {
        reader = new Reader();
        writer = new Writer();
        reader.start();
        writer.start();
    }

    public static <P extends SftpPath<P>> ClientHandler start(ByteChannel clientChannel, SftpFileSystem<P> fileSystem) {
        ClientHandler ret = new ClientHandler<>(clientChannel, fileSystem);
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
        private final Map<Integer, FileData<P>> openFiles = new HashMap<>();               // TODO: Limitare il numero di entries
        private final Map<Integer, DirectoryData<P>> openDirectories = new HashMap<>();    // TODO: Limitare il numero di entries
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
                        log.error("Unexpected exception processing {}", packet, ex);
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
            P path = SftpPath.parse(fileSystem, packet.getPath());
            try {
                Attrs attrs = createAttrs(path, packet.getuFlags(), LinkOption.NOFOLLOW_LINKS);
                writer.send(new SshFxpAttrs(packet.getuRequestId(), attrs));
            } catch (IOException e) {
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_FAILURE, e.getMessage(), "en"));
            }
        }

        @Override
        public void visit(SshFxpStat packet, Void parameter) {
            P path = SftpPath.parse(fileSystem, packet.getPath());
            try {
                Attrs attrs = createAttrs(path, packet.getuFlags());
                writer.send(new SshFxpAttrs(packet.getuRequestId(), attrs));
            } catch (IOException e) {
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_FAILURE, e.getMessage(), "en"));
            }
        }

        @Override
        public void visit(SshFxpFstat packet, Void parameter) {
            int handle = packet.getHandle().asInt();
            P path;
            if (openFiles.containsKey(handle)) {
                path = openFiles.get(handle).path;
            } else if (openDirectories.containsKey(handle)) {
                path = openDirectories.get(handle).path;
            } else {
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_INVALID_HANDLE, "Handle not found", "en"));
                return;
            }
            try {
                Attrs attrs = createAttrs(path, packet.getuFlags());
                writer.send(new SshFxpAttrs(packet.getuRequestId(), attrs));
            } catch (IOException e) {
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_FAILURE, e.getMessage(), "en"));
            }
        }

        @Override
        public void visit(SshFxpRealpath packet, Void parameter) {
            P path = SftpPath.parse(fileSystem, packet.getOriginalPath());
            for (String cp : packet.getComposePath()) {
                P pComponent = SftpPath.parse(fileSystem, cp);
                if (cp.charAt(0) == '/') {
                    path = pComponent;
                } else {
                    path = path.resolve(pComponent);
                }
            }
            path = path.normalize();
            switch (packet.getControlByte()) {
                case SSH_FXP_REALPATH_NO_CHECK:
                    writer.send(new SshFxpName(packet.getuRequestId(),
                            ImmutableList.of(SftpPath.toString(fileSystem, path)),
                            ImmutableList.of(Attrs.EMPTY),
                            Optional.of(true)));
                    break;
                case SSH_FXP_REALPATH_STAT_IF:
                    try {
                        path = path.toRealPath();
                        Attrs attrs = createAttrs(path);
                        writer.send(new SshFxpName(packet.getuRequestId(),
                                ImmutableList.of(SftpPath.toString(fileSystem, path)),
                                ImmutableList.of(attrs),
                                Optional.of(true)));
                    } catch (IOException e) {
                        writer.send(new SshFxpName(packet.getuRequestId(),
                                ImmutableList.of(SftpPath.toString(fileSystem, path)),
                                ImmutableList.of(Attrs.EMPTY),
                                Optional.of(true)));
                    }
                    break;

                case SSH_FXP_REALPATH_STAT_ALWAYS:
                    try {
                        path = path.toRealPath();
                        Attrs attrs = createAttrs(path);
                        writer.send(new SshFxpName(packet.getuRequestId(),
                                ImmutableList.of(SftpPath.toString(fileSystem, path)),
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
            P path = SftpPath.parse(fileSystem, packet.getPath());
            if (fileSystem.exists(path) && !fileSystem.isDirectory(path)) {
                writer.send(new SshFxpStatus(packet.getuRequestId(),
                        ErrorCode.SSH_FX_NOT_A_DIRECTORY,
                        null, null));
            } else {
                try {
                    DirectoryStream<P> dirStream = fileSystem.newDirectoryStream(path);
                    int handle = ++handlesCount;
                    openDirectories.put(handle, new DirectoryData<>(path, dirStream));
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
            DirectoryData<P> dirStream = openDirectories.get(packet.getHandle().asInt());
            if (dirStream == null) {
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_INVALID_HANDLE, "Handle not found", "en"));
            } else {
                ImmutableList.Builder<String> names = new ImmutableList.Builder<>();
                ImmutableList.Builder<Attrs> attributes = new ImmutableList.Builder<>();
                for (int i = 0; i < 16 && dirStream.iterator.hasNext(); i++) {
                    P path = dirStream.iterator.next();
                    names.add(path.getFileName());
                    try {
                        attributes.add(createAttrs(path));
                    } catch (IOException e) {
                        attributes.add(Attrs.EMPTY);
                    }
                }
                writer.send(new SshFxpName(packet.getuRequestId(), names.build(), attributes.build(), !dirStream.iterator.hasNext()));
            }
        }

        @Override
        public void visit(SshFxpOpen packet, Void parameter) {
            P fsPath = SftpPath.parse(fileSystem, packet.getFilename());
            try {
                boolean appendRequested = false;
                boolean truncateSet = false;
                ImmutableSet.Builder<StandardOpenOption> openOptions = ImmutableSet.builder();
                if (SshFxpOpen.AceMask.ACE4_READ_DATA.isSet(packet.getuDesideredAccess())) {
                    openOptions.add(StandardOpenOption.READ);
                }
                if (SshFxpOpen.AceMask.ACE4_WRITE_DATA.isSet(packet.getuDesideredAccess())) {
                    openOptions.add(StandardOpenOption.WRITE);
                }
                if (SshFxpOpen.AceMask.ACE4_APPEND_DATA.isSet(packet.getuDesideredAccess())) {
                    appendRequested = true;
                }
                if (SshFxpOpen.AceMask.ACE4_SYNCHRONIZE.isSet(packet.getuDesideredAccess())) {
                    openOptions.add(StandardOpenOption.DSYNC);
                }

                SshFxpOpen.OpenFlagsAccessDisposition accessDisposition = SshFxpOpen.OpenFlagsAccessDisposition.fromFlags(packet.getuFlags());
                switch (accessDisposition) {
                    case SSH_FXF_OPEN_OR_CREATE:
                        openOptions.add(StandardOpenOption.CREATE);
                        break;
                    case SSH_FXF_CREATE_NEW:
                        openOptions.add(StandardOpenOption.CREATE_NEW);
                        break;
                    case SSH_FXF_CREATE_TRUNCATE:
                        openOptions.add(StandardOpenOption.TRUNCATE_EXISTING)
                                .add(StandardOpenOption.CREATE);
                        truncateSet = true;
                        break;
                    case SSH_FXF_OPEN_EXISTING:
                        // No extra options for this
                        break;
                    case SSH_FXF_TRUNCATE_EXISTING:
                        openOptions.add(StandardOpenOption.TRUNCATE_EXISTING);
                        truncateSet = true;
                        break;
                    default:
                        throw new IllegalStateException("Unsupported access disposition " + accessDisposition);
                }
                if (SshFxpOpen.OpenFlags.SSH_FXF_APPEND_DATA.isSet(packet.getuFlags())) {
                    appendRequested = true;
                }
                if (SshFxpOpen.OpenFlags.SSH_FXF_DELETE_ON_CLOSE.isSet(packet.getuFlags())) {
                    openOptions.add(StandardOpenOption.DELETE_ON_CLOSE);
                }

                if (appendRequested && !truncateSet) {
                    // Both flags are not supported by Java
                    openOptions.add(StandardOpenOption.APPEND);
                }

                ImmutableSet<StandardOpenOption> bOpenOptions = openOptions.build();
                SeekableByteChannel fileChannel = fileSystem.newByteChannel(fsPath, bOpenOptions);
                int handle = ++handlesCount;
                openFiles.put(handle, new FileData<>(fileChannel, fsPath, appendRequested));
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

        @Override
        public void visit(SshFxpWrite packet, Void parameter) {
            FileData fileData = openFiles.get(packet.getHandle().asInt());
            if (fileData == null) {
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_INVALID_HANDLE, "Handle not found", "en"));
                return;
            }
            try {
                if (!fileData.append) {
                    fileData.channel.position(packet.getuOffset());
                }
                ByteBuffer toWrite = packet.getData().asBuffer();
                while (toWrite.hasRemaining()) {
                    fileData.channel.write(toWrite);
                }
                writer.send(new SshFxpStatus(packet.getuRequestId(), ErrorCode.SSH_FX_OK, "", ""));
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

    private static final class DirectoryData<P> implements Closeable {
        public final DirectoryStream<P> stream;
        public final Iterator<P> iterator;
        public final P path;

        public DirectoryData(P path, DirectoryStream<P> stream) {
            this.path = path;
            this.stream = stream;
            this.iterator = stream.iterator();
        }

        @Override
        public void close() throws IOException {
            stream.close();
        }
    }

    private static final class FileData<P extends SftpPath> implements Closeable {
        public final SeekableByteChannel channel;
        public final P path;
        public final boolean append;

        public FileData(SeekableByteChannel channel, P path, boolean append) {
            this.channel = channel;
            this.path = path;
            this.append = append;
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }
    }
}

