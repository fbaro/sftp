package it.ftb.sftp;

import com.google.common.collect.ImmutableList;
import it.ftb.sftp.packet.PacketType;
import it.ftb.sftp.packet.SshFxpRealpath;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultPacketWriterTest {

    @Test
    public void testWritingShortPacket() {
        HoldingChannel output = new HoldingChannel();
        DefaultPacketWriter encoder = new DefaultPacketWriter(output);
        encoder.visitStat(0xcafebabe, "0123", 0xdeadbeef);
        assertEquals(1, output.buffers.size());
        assertEquals(ByteBuffer.wrap(hexToBytes("0000001111cafebabe0000000430313233deadbeef")), output.buffers.get(0));
    }

    @Test
    public void testWritingLongPacket() {
        HoldingChannel output = new HoldingChannel();
        DefaultPacketWriter encoder = new DefaultPacketWriter(output);
        ImmutableList.Builder<String> b = new ImmutableList.Builder<>();
        for (int i = 0; i < 10000; i++) {
            b.add("0123456789ab");
        }
        encoder.visitRealpath(0x1, ".", SshFxpRealpath.ControlByte.SSH_FXP_REALPATH_NO_CHECK,
                b.build());
        int expectedLength = 1 + 4 + 5 + 1 + 16 * 10000;
        assertTrue(output.buffers.size() >= 1);
        assertEquals(expectedLength, output.buffers.get(0).getInt(0));
        assertEquals(PacketType.SSH_FXP_REALPATH.getCode(), output.buffers.get(0).get(4));
        assertEquals(4 + expectedLength, output.buffers.stream().mapToInt(ByteBuffer::remaining).sum());
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

    private static final class HoldingChannel implements Consumer<ByteBuffer> {

        final List<ByteBuffer> buffers = new ArrayList<>();

        @Override
        public void accept(ByteBuffer src) {
            ByteBuffer copy = ByteBuffer.allocate(src.remaining());
            copy.put(src);
            copy.flip();
            buffers.add(copy);
        }
    }
}