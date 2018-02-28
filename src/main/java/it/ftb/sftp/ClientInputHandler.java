package it.ftb.sftp;

import it.ftb.sftp.packet.AbstractPacket;
import it.ftb.sftp.packet.PacketType;
import it.ftb.sftp.packet.VoidPacketVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Handles incoming from the client. Splits the data in packets, and sends the decoded packets to a processor.
 * This class allows both for thread-style and event-style processing.
 */
public class ClientInputHandler implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ClientInputHandler.class);

    private final ByteBuffer clientBuffer = ByteBuffer.allocate(0x10000);
    private final VoidPacketVisitor processor;

    /**
     * Creates a new ClientInputHandler, splitting packets and sending them to a processor.
     * @param processor The processor to send packets to
     */
    public ClientInputHandler(VoidPacketVisitor processor) {
        this.processor = processor;
    }

    /**
     * Accepts and processes new data from the client. Upon exiting the method, the <i>data</i> buffer will have
     * been emptied.
     * @param data The data received from the client
     */
    public void receive(ByteBuffer data) {
        LOG.trace("Received {} bytes", data.remaining());
        if (clientBuffer.position() != 0) {
            clientBuffer.put(data);
            clientBuffer.flip();
            process(clientBuffer);
            clientBuffer.compact();
        } else {
            process(data);
            clientBuffer.put(data);
        }
    }

    /**
     * Signals the client has closed its side of the connection
     */
    @Override
    public void close() {
        LOG.trace("Closed");
        // TODO?
    }

    /**
     * Reads and processes all the complete packets in <i>data</i>. When returning, the buffer will have its
     * position set at the first unprocessed packet.
     *
     * @param data The data to be processed
     */
    private void process(ByteBuffer data) {
        while (data.remaining() > 4) {
            int length = data.getInt(0);
            if (data.remaining() < length + 4) {
                break;
            }
            processPacket(data, processor);
        }
    }

    private static <V> void processPacket(ByteBuffer data, VoidPacketVisitor processor) {
        int length = data.getInt();
        PacketDecoder packetDecoder = new PacketDecoder(new BufferDecoder(data), length);
        int packetCode = packetDecoder.readByte() & 0xff;
        PacketType packetType = PacketType.fromCode(packetCode);
        if (packetType == null || packetType.getPacketFactory() == null) {
            LOG.warn("Ignoring unsupported packet type " + packetCode);
        } else {
            AbstractPacket packet = packetType.getPacketFactory().read(packetDecoder);
            LOG.debug("Received packet {}", packet);
            packet.visit(processor);
        }
    }
}
