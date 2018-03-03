package it.ftb.sftp.packet;

import it.ftb.sftp.network.Decoder;

public final class SshFxpLstat {

    public static final PacketFactory<SshFxpLstat> FACTORY = new PacketFactory<SshFxpLstat>() {
        @Override
        public void read(Decoder decoder, VoidPacketVisitor visitor) {
            int requestId = decoder.readInt();
            String path = decoder.readString().getString();
            int uFlags = decoder.readInt();
            visitor.visitLstat(requestId, path, uFlags);
        }
    };
}
