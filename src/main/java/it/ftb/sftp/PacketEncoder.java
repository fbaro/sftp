package it.ftb.sftp;

import it.ftb.sftp.network.Bytes;
import it.ftb.sftp.network.Encoder;
import it.ftb.sftp.packet.AbstractPacket;
import it.ftb.sftp.packet.PacketType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class PacketEncoder {

    private final WritableByteChannel channel;
    private final DumpingEncoder dumpingEncoder = new DumpingEncoder();
    private final WritingEncoder writingEncoder = new WritingEncoder();
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(0x10000);
    private int flushedBytes = 0;

    public PacketEncoder(WritableByteChannel channel) {
        this.channel = channel;
    }

    public void write(AbstractPacket packet) {
        write(packet.getType(), packet::write);
    }

    public void write(PacketType type, Consumer<Encoder> writer) {
        buffer.position(4);
        buffer.put((byte) type.getCode());
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
            buffer.put((byte) type.getCode());
            writer.accept(writingEncoder);
        }
        flushBuffer();
    }

    private void flushBuffer() {
        buffer.flip();
        while (buffer.hasRemaining()) {
            try {
                channel.write(buffer);
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }
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
}
