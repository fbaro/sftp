package it.ftb.sftp;

import it.ftb.sftp.network.Bytes;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.StringWithLength;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

final class ChannelDecoder implements Decoder {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static final Optional<Boolean> OPT_TRUE = Optional.of(true);
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static final Optional<Boolean> OPT_FALSE = Optional.of(false);

    private final ReadableByteChannel channel;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(0x10000).order(ByteOrder.BIG_ENDIAN);

    ChannelDecoder(ReadableByteChannel channel) {
        this.channel = channel;
        this.buffer.flip();
    }

    private boolean gather(int numBytes, boolean optional) {
        int totRead = 0;
        while (buffer.limit() < buffer.capacity() && buffer.remaining() < numBytes) {
            buffer.compact();
            try {
                int numRead;
                if ((numRead = channel.read(buffer)) == -1) {
                    if (totRead == 0 && optional) {
                        return false;
                    } else {
                        throw new EndOfChannelException();
                    }
                }
                totRead += numRead;
            } catch (IOException ex) {
                throw new RuntimeIOException(ex);
            }
            buffer.flip();
        }
        if (buffer.remaining() < numBytes && numBytes <= 8) {
            throw new IllegalStateException("No space in buffer for primitive read");
        }
        return true;
    }

    @Override
    public OptionalInt readOptByte() {
        if (!gather(1, true)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(buffer.get());
    }

    @Override
    public OptionalInt readOptInt() {
        if (!gather(4, true)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(buffer.getInt());
    }

    @Override
    public OptionalLong readOptLong() {
        if (!gather(8, true)) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(buffer.getLong());
    }

    @Override
    public Optional<StringWithLength> readOptString() {
        if (!gather(4, true)) {
            return Optional.empty();
        }
        int len = buffer.getInt();
        return Optional.of(new StringWithLength(len, StandardCharsets.UTF_8.decode(readBytes(len)).toString()));
    }

    @Override
    public Optional<Boolean> readOptBoolean() {
        if (!gather(1, true)) {
            return Optional.empty();
        }
        return (buffer.get() == 0 ? OPT_FALSE : OPT_TRUE);
    }

    @Override
    public StringWithLength readString() {
        gather(4, false);
        int len = buffer.getInt();
        return new StringWithLength(len, StandardCharsets.UTF_8.decode(readBytes(len)).toString());
    }

    @Override
    public Bytes readBytes() {
        gather(4, false);
        int len = buffer.getInt();
        return Bytes.copy(readBytes(len), len);
    }

    private ByteBuffer readBytes(int len) {
        gather(len, false);
        if (buffer.remaining() == len) {
            return buffer;
        } else if (buffer.remaining() > len) {
            ByteBuffer ret = buffer.duplicate();
            ret.limit(ret.position() + len);
            buffer.position(ret.limit());
            return ret;
        } else {
            ByteBuffer tmp = ByteBuffer.allocate(len);
            do {
                tmp.put(buffer);
                gather(tmp.remaining(), false);
            } while (tmp.hasRemaining());
            tmp.flip();
            return tmp;
        }
    }

    @Override
    public int readInt() {
        gather(4, false);
        return buffer.getInt();
    }

    @Override
    public long readLong() {
        gather(8, false);
        return buffer.getLong();
    }

    @Override
    public boolean readBoolean() {
        gather(1, false);
        return buffer.get() != 0;
    }

    @Override
    public byte readByte() {
        gather(1, false);
        return buffer.get();
    }
}
