package it.ftb.sftp.packet;

import it.ftb.sftp.network.Bytes;
import it.ftb.sftp.network.Decoder;

public final class SshFxpFstat {

    public static final PacketFactory<SshFxpFstat> FACTORY = new PacketFactory<SshFxpFstat>() {
        @Override
        public void read(Decoder decoder, VoidPacketVisitor visitor) {
            int requestId = decoder.readInt();
            Bytes handle = decoder.readBytes();
            int uFlags = decoder.readInt();
            visitor.visitFstat(requestId, handle, uFlags);
        }
    };
}
