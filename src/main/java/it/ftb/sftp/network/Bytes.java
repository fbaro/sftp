package it.ftb.sftp.network;

import java.nio.ByteBuffer;

public abstract class Bytes {

    public abstract int getLength();

    public int asInt() {
        ByteBuffer b = asBuffer();
        if (b.remaining() != 4) {
            throw new IllegalStateException();
        }
        return b.getInt();
    }

    public abstract ByteBuffer asBuffer();

    public abstract void copyTo(ByteBuffer buffer);

    private Bytes() {
    }

    public static Bytes from(int value) {
        return new Bytes() {
            @Override
            public int getLength() {
                return 4;
            }

            @Override
            public ByteBuffer asBuffer() {
                ByteBuffer ret = ByteBuffer.allocate(4);
                ret.putInt(value);
                ret.flip();
                return ret;
            }

            @Override
            public int asInt() {
                return value;
            }

            @Override
            public void copyTo(ByteBuffer buffer) {
                buffer.putInt(value);
            }
        };
    }

    public static Bytes copy(ByteBuffer buffer, final int count) {
        final ByteBuffer copy = ByteBuffer.allocate(count);
        int l = buffer.limit();
        buffer.limit(buffer.position() + Math.min(buffer.remaining(), count));
        copy.put(buffer);
        buffer.limit(l);
        copy.flip();
        return new Bytes() {
            @Override
            public int getLength() {
                return count;
            }

            @Override
            public ByteBuffer asBuffer() {
                return copy;
            }

            @Override
            public void copyTo(ByteBuffer buffer) {
                buffer.put(copy.duplicate());
            }
        };
    }

    public static Bytes hold(ByteBuffer buffer) {
        final ByteBuffer sliced = buffer.slice();
        return new Bytes() {
            @Override
            public int getLength() {
                return sliced.capacity();
            }

            @Override
            public ByteBuffer asBuffer() {
                return sliced.duplicate();
            }

            @Override
            public void copyTo(ByteBuffer buffer) {
                buffer.put(sliced.duplicate());
            }
        };
    }
}