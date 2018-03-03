package it.ftb.sftp.packet;

import it.ftb.sftp.network.Bytes;
import it.ftb.sftp.network.Decoder;

public class SshFxpClose {

    public static final PacketFactory<SshFxpClose> FACTORY = new PacketFactory<SshFxpClose>() {
        @Override
        public void read(Decoder decoder, VoidPacketVisitor visitor) {
            int requestId = decoder.readInt();
            Bytes handle = decoder.readBytes();
            visitor.visitClose(requestId, handle);
        }
    };
}