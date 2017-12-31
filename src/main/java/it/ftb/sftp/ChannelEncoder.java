package it.ftb.sftp;

import it.ftb.sftp.network.Bytes;
import it.ftb.sftp.network.Encoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;

public final class ChannelEncoder implements Encoder {

    private final ByteBuffer buffer = ByteBuffer.allocateDirect(0x10000);
    private final WritableByteChannel channel;

    public ChannelEncoder(WritableByteChannel channel) {
        this.channel = channel;
    }

    private void ensureSpace(int bytes) {
        if (buffer.remaining() < bytes) {
            flush();
        }
    }

    public void flush() {
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

    @Override
    public Encoder write(byte b) {
        ensureSpace(1);
        buffer.put(b);
        return this;
    }

    @Override
    public Encoder write(int i) {
        ensureSpace(1);
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
    public Encoder write(String s) {
        ensureSpace(4 + s.length() * 2);
        int p = buffer.position();
        buffer.position(p + 4);
        CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
        CharBuffer cb = CharBuffer.wrap(s);
        CoderResult result = encoder.encode(cb, buffer, true);
        if (result.isError() | result.isUnderflow() | result.isOverflow()) {
            try {
                result.throwException();
            } catch (CharacterCodingException e) {
                throw new RuntimeIOException(e);
            }
        }
        buffer.putInt(p, buffer.position() - p - 4);
        return this;
    }

    @Override
    public Encoder write(Bytes b) {
        ensureSpace(4 + b.getLength());
        buffer.putInt(b.getLength());
        b.copyTo(buffer);
        return this;
    }
}
