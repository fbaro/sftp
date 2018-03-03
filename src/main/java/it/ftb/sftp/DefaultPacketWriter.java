package it.ftb.sftp;

import com.google.common.collect.ImmutableList;
import it.ftb.sftp.network.Bytes;
import it.ftb.sftp.network.Encoder;
import it.ftb.sftp.packet.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;

public final class DefaultPacketWriter implements VoidPacketVisitor {

    private final DumpingEncoder dumpingEncoder = new DumpingEncoder();
    private final WritingEncoder writingEncoder = new WritingEncoder();
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(0x10000);
    private final Consumer<ByteBuffer> networkSend;
    private int flushedBytes = 0;

    public DefaultPacketWriter(Consumer<ByteBuffer> networkSend) {
        this.networkSend = networkSend;
    }

    @Override
    public void visit() {
        throw new UnsupportedOperationException("Unsupported packet type");
    }

    @Override
    public void visitVersion(int uVersion, ImmutableList<ExtensionPair> extensions) {
        write(encoder -> new NoLengthPacketWriter(encoder).visitVersion(uVersion, extensions));
    }

    @Override
    public void visitOpen(int uRequestId, String filename, int uDesideredAccess, int uFlags, Attrs attrs) {
        write(encoder -> new NoLengthPacketWriter(encoder).visitOpen(uRequestId, filename, uDesideredAccess, uFlags, attrs));
    }

    @Override
    public void visitOpenDir(int uRequestId, String path) {
        write(encoder -> new NoLengthPacketWriter(encoder).visitOpenDir(uRequestId, path));
    }

    @Override
    public void visitHandle(int uRequestId, Bytes handle) {
        write(encoder -> new NoLengthPacketWriter(encoder).visitHandle(uRequestId, handle));
    }

    @Override
    public void visitClose(int uRequestId, Bytes handle) {
        write(encoder -> new NoLengthPacketWriter(encoder).visitClose(uRequestId, handle));
    }

    @Override
    public void visitReadDir(int uRequestId, Bytes handle) {
        write(encoder -> new NoLengthPacketWriter(encoder).visitReadDir(uRequestId, handle));
    }

    @Override
    public void visitRead(int uRequestId, Bytes handle, long uOffset, int uLength) {
        write(encoder -> new NoLengthPacketWriter(encoder).visitRead(uRequestId, handle, uOffset, uLength));
    }

    @Override
    public void visitData(int uRequestId, Bytes data, boolean endOfFile) {
        write(encoder -> new NoLengthPacketWriter(encoder).visitData(uRequestId, data, endOfFile));
    }

    @Override
    public void visitStatus(int uRequestId, ErrorCode errorCode, String errorMessage, String errorMessageLanguage) {
        write(encoder -> new NoLengthPacketWriter(encoder).visitStatus(uRequestId, errorCode, errorMessage, errorMessageLanguage));
    }

    @Override
    public void visitName(int uRequestId, ImmutableList<String> names, ImmutableList<Attrs> attributes, Optional<Boolean> endOfList) {
        write(encoder -> new NoLengthPacketWriter(encoder).visitName(uRequestId, names, attributes, endOfList));
    }

    @Override
    public void visitLstat(int uRequestId, String path, int uFlags) {
        write(encoder -> new NoLengthPacketWriter(encoder).visitLstat(uRequestId, path, uFlags));
    }

    @Override
    public void visitStat(int uRequestId, String path, int uFlags) {
        write(encoder -> new NoLengthPacketWriter(encoder).visitStat(uRequestId, path, uFlags));
    }

    @Override
    public void visitFstat(int uRequestId, Bytes handle, int uFlags) {
        write(encoder -> new NoLengthPacketWriter(encoder).visitFstat(uRequestId, handle, uFlags));
    }

    @Override
    public void visitRealpath(int uRequestId, String originalPath, SshFxpRealpath.ControlByte controlByte, ImmutableList<String> composePath) {
        write(encoder -> new NoLengthPacketWriter(encoder).visitRealpath(uRequestId, originalPath, controlByte, composePath));
    }

    @Override
    public void visitAttrs(int uRequestId, Attrs attrs) {
        write(encoder -> new NoLengthPacketWriter(encoder).visitAttrs(uRequestId, attrs));
    }

    @Override
    public void visitWrite(int uRequestId, Bytes handle, long uOffset, Bytes data) {
        write(encoder -> new NoLengthPacketWriter(encoder).visitWrite(uRequestId, handle, uOffset, data));
    }

    @Override
    public void visitSetstat(int uRequestId, String path, Attrs attrs) {
        write(encoder -> new NoLengthPacketWriter(encoder).visitSetstat(uRequestId, path, attrs));
    }

    /**
     * Package private only for testing.
     *
     * @param writer Method to write the required bytes
     */
    void write(Consumer<Encoder> writer) {
        buffer.position(4);
        flushedBytes = 0;

        // We need to first write the packet length.
        // We write the packet to the internal buffer; if it fits, we can roll back to add
        // the length, and write the whole buffer to the channel.
        // If it does not fit, we clear the written data and keep track of the number of bytes.
        // We then know the bytes count, and make a second pass to actually write the data
        // to the channel while it is produced.
        writer.accept(dumpingEncoder);
        if (flushedBytes == 0) {
            buffer.putInt(0, buffer.position() - 4);
        } else {
            int actualLength = flushedBytes - 4 + buffer.position();
            buffer.clear();
            buffer.putInt(actualLength);
            writer.accept(writingEncoder);
        }
        flushBuffer();
    }

    private void flushBuffer() {
        buffer.flip();
        while (buffer.hasRemaining()) {
            networkSend.accept(buffer);
        }
        buffer.clear();
    }

    private abstract class BufferingEncoder implements Encoder {

        protected abstract void ensureSpace(int bytes);

        @Override
        public Encoder write(byte b) {
            ensureSpace(1);
            buffer.put(b);
            return this;
        }

        @Override
        public Encoder write(int i) {
            ensureSpace(4);
            buffer.putInt(i);
            return this;
        }

        @Override
        public Encoder write(long l) {
            ensureSpace(8);
            buffer.putLong(l);
            return this;
        }

        @Override
        public Encoder write(boolean b) {
            ensureSpace(1);
            buffer.put(b ? (byte) 1 : (byte) 0);
            return this;
        }

        @Override
        public Encoder write(Bytes b) {
            ensureSpace(4 + b.getLength());
            buffer.putInt(b.getLength());
            b.copyTo(buffer);
            return this;
        }

        @Override
        public Encoder write(String s) {
            if (s == null) {
                ensureSpace(4);
                buffer.putInt(0);
                return this;
            }
            CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
            ensureSpace(4 + (int) (s.length() * encoder.maxBytesPerChar()));
            int p = buffer.position();
            buffer.position(p + 4);
            CharBuffer cb = CharBuffer.wrap(s);
            CoderResult result = encoder.encode(cb, buffer, true);
            if (result.isError() | result.isOverflow()) {
                try {
                    result.throwException();
                } catch (CharacterCodingException e) {
                    throw new RuntimeIOException(e);
                }
            }
            buffer.putInt(p, buffer.position() - p - 4);
            return this;
        }
    }

    private final class DumpingEncoder extends BufferingEncoder {

        protected void ensureSpace(int bytes) {
            if (buffer.remaining() < bytes) {
                flushedBytes += buffer.position();
                buffer.clear();
            }
        }
    }

    private final class WritingEncoder extends BufferingEncoder {

        protected void ensureSpace(int bytes) {
            if (buffer.remaining() < bytes) {
                flushBuffer();
            }
        }
    }

    public static final class NoLengthPacketWriter implements VoidPacketVisitor {

        private final Encoder enc;

        public NoLengthPacketWriter(Encoder encoder) {
            this.enc = encoder;
        }

        @Override
        public void visit() {
            throw new UnsupportedOperationException("Unknown packet type");
        }

        @Override
        public void visitInit(int uVersion) {
            enc.write(PacketType.SSH_FXP_INIT.getCodeAsByte());
            enc.write(uVersion);
        }

        @Override
        public void visitVersion(int uVersion, ImmutableList<ExtensionPair> extensions) {
            enc.write(PacketType.SSH_FXP_VERSION.getCodeAsByte());
            enc.write(uVersion);
            extensions.forEach(ep -> ep.write(enc));
        }

        @Override
        public void visitOpen(int uRequestId, String filename, int uDesideredAccess, int uFlags, Attrs attrs) {
            enc.write(PacketType.SSH_FXP_OPEN.getCodeAsByte());
            enc.write(uRequestId);
            enc.write(filename);
            enc.write(uDesideredAccess);
            enc.write(uFlags);
            attrs.write(enc);
        }

        @Override
        public void visitOpenDir(int uRequestId, String path) {
            enc.write(PacketType.SSH_FXP_OPENDIR.getCodeAsByte());
            enc.write(uRequestId);
            enc.write(path);
        }

        @Override
        public void visitHandle(int uRequestId, Bytes handle) {
            enc.write(PacketType.SSH_FXP_HANDLE.getCodeAsByte());
            enc.write(uRequestId)
                    .write(handle);
        }

        @Override
        public void visitClose(int uRequestId, Bytes handle) {
            enc.write(PacketType.SSH_FXP_CLOSE.getCodeAsByte());
            enc.write(uRequestId)
                    .write(handle);
        }

        @Override
        public void visitReadDir(int uRequestId, Bytes handle) {
            enc.write(PacketType.SSH_FXP_READDIR.getCodeAsByte());
            enc.write(uRequestId);
            enc.write(handle);
        }

        @Override
        public void visitRead(int uRequestId, Bytes handle, long uOffset, int uLength) {
            enc.write(PacketType.SSH_FXP_READ.getCodeAsByte());
            enc.write(uRequestId)
                    .write(handle)
                    .write(uOffset)
                    .write(uLength);
        }

        @Override
        public void visitData(int uRequestId, Bytes data, boolean endOfFile) {
            enc.write(PacketType.SSH_FXP_DATA.getCodeAsByte());
            enc.write(uRequestId)
                    .write(data);
            if (endOfFile) {
                enc.write(true);
            }
        }

        @Override
        public void visitStatus(int uRequestId, ErrorCode errorCode, String errorMessage, String errorMessageLanguage) {
            enc.write(PacketType.SSH_FXP_STATUS.getCodeAsByte());
            enc.write(uRequestId)
                    .write(errorCode.getCode())
                    .write(errorMessage)
                    .write(errorMessageLanguage);
        }

        @Override
        public void visitName(int uRequestId, ImmutableList<String> names, ImmutableList<Attrs> attributes, Optional<Boolean> endOfList) {
            enc.write(PacketType.SSH_FXP_NAME.getCodeAsByte());
            enc.write(uRequestId);
            enc.write(names.size());
            for (int i = 0; i < names.size(); i++) {
                enc.write(names.get(i));
                attributes.get(i).write(enc);
            }
            endOfList.ifPresent(enc::write);
        }

        @Override
        public void visitLstat(int uRequestId, String path, int uFlags) {
            enc.write(PacketType.SSH_FXP_LSTAT.getCodeAsByte());
            enc.write(uRequestId);
            enc.write(path);
            enc.write(uFlags);
        }

        @Override
        public void visitStat(int uRequestId, String path, int uFlags) {
            enc.write(PacketType.SSH_FXP_STAT.getCodeAsByte());
            enc.write(uRequestId);
            enc.write(path);
            enc.write(uFlags);
        }

        @Override
        public void visitFstat(int uRequestId, Bytes handle, int uFlags) {
            enc.write(PacketType.SSH_FXP_FSTAT.getCodeAsByte());
            enc.write(uRequestId);
            enc.write(handle);
            enc.write(uFlags);
        }

        @Override
        public void visitRealpath(int uRequestId, String originalPath, SshFxpRealpath.ControlByte controlByte, ImmutableList<String> composePath) {
            enc.write(PacketType.SSH_FXP_REALPATH.getCodeAsByte());
            enc.write(uRequestId);
            enc.write(originalPath);
            if (controlByte == SshFxpRealpath.ControlByte.SSH_FXP_REALPATH_NO_CHECK
                    && composePath.isEmpty()) {
                return;
            }
            enc.write((byte) controlByte.getCode());
            for (String cp : composePath) {
                enc.write(cp);
            }
        }

        @Override
        public void visitAttrs(int uRequestId, Attrs attrs) {
            enc.write(PacketType.SSH_FXP_ATTRS.getCodeAsByte());
            enc.write(uRequestId);
            attrs.write(enc);
        }

        @Override
        public void visitWrite(int uRequestId, Bytes handle, long uOffset, Bytes data) {
            enc.write(PacketType.SSH_FXP_WRITE.getCodeAsByte());
            enc.write(uRequestId)
                    .write(handle)
                    .write(uOffset)
                    .write(data);
        }

        @Override
        public void visitSetstat(int uRequestId, String path, Attrs attrs) {
            enc.write(PacketType.SSH_FXP_SETSTAT.getCodeAsByte());
            enc.write(uRequestId);
            enc.write(path);
            attrs.write(enc);
        }
    }
}
