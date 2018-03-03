package it.ftb.sftp.packet;

import it.ftb.sftp.network.Bytes;
import it.ftb.sftp.network.Decoder;

public class SshFxpWrite {

    public static final PacketFactory<SshFxpWrite> FACTORY = new PacketFactory<SshFxpWrite>() {
        @Override
        public void read(Decoder decoder, VoidPacketVisitor visitor) {
            int requestId = decoder.readInt();
            Bytes handle = decoder.readBytes();
            long offset = decoder.readLong();
            Bytes data = decoder.readBytes();
            visitor.visitWrite(requestId, handle, offset, data);
        }
    };
}
