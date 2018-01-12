package it.ftb.sftp;

import it.ftb.sftp.network.Bytes;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.MalformedPacketException;
import it.ftb.sftp.network.StringWithLength;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Predicate;

final class PacketDecoder implements Decoder {

    private final Decoder delegate;
    private int length;

    PacketDecoder(Decoder delegate, int length) {
        this.delegate = delegate;
        this.length = length;
    }

    private <T> T checkOptional(T optional, int size, Predicate<T> isPresent) {
        if (isPresent.test(optional)) {
            length -= size;
            if (length < 0) {
                throw new IllegalStateException("Reading past packet boundary");
            }
        }
        return optional;
    }

    private void checkRemaining(int size) {
        if (length < size) {
            throw new IllegalStateException("Reading past packet boundary");
        }
        length -= size;
    }

    @Override
    public OptionalInt readOptByte() {
        if (length == 0) {
            return OptionalInt.empty();
        }
        return checkOptional(delegate.readOptByte(), 1, OptionalInt::isPresent);
    }

    @Override
    public OptionalInt readOptInt() {
        if (length == 0) {
            return OptionalInt.empty();
        }
        return checkOptional(delegate.readOptInt(), 4, OptionalInt::isPresent);
    }

    @Override
    public OptionalLong readOptLong() {
        if (length == 0) {
            return OptionalLong.empty();
        }
        return checkOptional(delegate.readOptLong(), 8, OptionalLong::isPresent);
    }

    @Override
    public Optional<StringWithLength> readOptString() {
        if (length == 0) {
            return Optional.empty();
        }
        if (length < 4) {
            throw new MalformedPacketException("Not enough bytes for a string");
        }
        StringWithLength ret = delegate.readString();
        checkRemaining(4 + ret.getLength());
        return Optional.of(ret);
    }

    @Override
    public Optional<Boolean> readOptBoolean() {
        if (length == 0) {
            return Optional.empty();
        }
        return checkOptional(delegate.readOptBoolean(), 8, Optional::isPresent);
    }

    @Override
    public StringWithLength readString() {
        StringWithLength ret = delegate.readString();
        checkRemaining(4 + ret.getLength());
        return ret;
    }

    @Override
    public Bytes readBytes() {
        Bytes ret = delegate.readBytes();
        checkRemaining(ret.getLength());
        return ret;
    }

    @Override
    public int readInt() {
        checkRemaining(4);
        return delegate.readInt();
    }

    @Override
    public long readLong() {
        checkRemaining(8);
        return delegate.readLong();
    }

    @Override
    public boolean readBoolean() {
        checkRemaining(1);
        return delegate.readBoolean();
    }

    @Override
    public byte readByte() {
        checkRemaining(1);
        return delegate.readByte();
    }

    public void skipRemaining() {

    }
}
