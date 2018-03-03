package it.ftb.sftp.packet;

import it.ftb.sftp.network.Bytes;
import it.ftb.sftp.network.Decoder;

public class SshFxpHandle {

    public static final PacketFactory<SshFxpHandle> FACTORY = new PacketFactory<SshFxpHandle>() {
        @Override
        public void read(Decoder decoder, VoidPacketVisitor visitor) {
            int requestId = decoder.readInt();
            Bytes handle = decoder.readBytes();
            visitor.visitHandle(requestId, handle);
        }
    };
}
