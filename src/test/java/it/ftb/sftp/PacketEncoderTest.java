package it.ftb.sftp;

import it.ftb.sftp.packet.PacketType;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PacketEncoderTest {

    @Test
    public void testWritingShortPacket() {
        HoldingChannel output = new HoldingChannel();
        PacketEncoder encoder = new PacketEncoder(output);
        encoder.write(PacketType.SSH_FXP_INIT, enc -> {
            enc.write(0xcafebabe);
            enc.write("0123");
        });
        assertEquals(1, output.buffers.size());
        assertEquals(ByteBuffer.wrap(hexToBytes("0000000d01cafebabe0000000430313233")), output.buffers.get(0));
    }

    @Test
    public void testWritingLongPacket() {
        HoldingChannel output = new HoldingChannel();
        PacketEncoder encoder = new PacketEncoder(output);
        encoder.write(PacketType.SSH_FXP_INIT, enc -> {
            for (int i = 0; i < 10000; i++) { //
                enc.write("0123456789ab"); // 4 + 12 bytes
            }
        });
        assertTrue(output.buffers.size() >= 1);
        assertEquals(1 + 16 * 10000, output.buffers.get(0).getInt(0));
        assertEquals(PacketType.SSH_FXP_INIT.getCode(), output.buffers.get(0).get(4));
        assertEquals(4 + 1 + 16 * 10000, output.buffers.stream().mapToInt(ByteBuffer::remaining).sum());
    }

    private static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static final class HoldingChannel implements WritableByteChannel {

        final List<ByteBuffer> buffers = new ArrayList<>();

        @Override
        public int write(ByteBuffer src) {
            ByteBuffer copy = ByteBuffer.allocate(src.remaining());
            copy.put(src);
            copy.flip();
            buffers.add(copy);
            return copy.remaining();
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void close() {
        }
    }
}