package it.ftb.sftp.packet;

import it.ftb.sftp.network.Bytes;
import it.ftb.sftp.network.Decoder;

public class SshFxpRead {

    public static final PacketFactory<SshFxpRead> FACTORY = new PacketFactory<SshFxpRead>() {
        @Override
        public void read(Decoder decoder, VoidPacketVisitor visitor) {
            int requestId = decoder.readInt();
            Bytes handle = decoder.readBytes();
            long offset = decoder.readLong();
            int length = decoder.readInt();
            visitor.visitRead(requestId, handle, offset, length);
        }
    };
}
