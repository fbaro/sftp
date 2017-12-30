package it.ftb.sftp.network;

import com.google.common.base.Charsets;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Optional;
import java.util.OptionalInt;

public class ByteBufferDecoder implements Decoder {

    private final ByteBuffer buffer;

    public ByteBufferDecoder(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public OptionalInt readOptByte() {
        if (!buffer.hasRemaining()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(buffer.get());
    }

    @Override
    public OptionalInt readOptInt32() {
        if (!buffer.hasRemaining()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(buffer.getInt());
        } catch (BufferUnderflowException e) {
            throw new MalformedPacketException("Partial data decoding int");
        }
    }

    @Override
    public Optional<String> readOptString() {
        if (!buffer.hasRemaining()) {
            return Optional.empty();
        }
        return Optional.of(_readString());
    }

    @Override
    public String readString() {
        if (!buffer.hasRemaining()) {
            throw new MalformedPacketException("End of data reading string");
        }
        return _readString();
    }

    private String _readString() {
        try {
            int len = buffer.getInt();
            ByteBuffer b = buffer.duplicate();
            b.limit(b.position() + len);
            CharsetDecoder decoder = Charsets.UTF_8.newDecoder();
            String result = decoder.onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(b)
                    .toString();
            b.position(b.position() + len);
            return result;
        } catch (BufferUnderflowException e) {
            throw new MalformedPacketException("Partial data decoding string");
        } catch (CharacterCodingException e) {
            throw new MalformedPacketException("Error decoding UTF-8 string");
        }
    }

    @Override
    public int readInt() {
        if (!buffer.hasRemaining()) {
            throw new MalformedPacketException("End of data reading int");
        }
        try {
            return buffer.getInt();
        } catch (BufferUnderflowException e) {
            throw new MalformedPacketException("Partial data decoding int");
        }
    }
}
