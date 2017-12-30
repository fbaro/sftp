package it.ftb.sftp.network;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Bytes {

    private final byte[] value;

    public Bytes(byte[] value) {
        this.value = Arrays.copyOf(value, value.length);
    }

    public Bytes(ByteBuffer buffer, int count) {
        this.value = new byte[count];
        buffer.get(this.value);
    }

    public int getLength() {
        return value.length;
    }
}
