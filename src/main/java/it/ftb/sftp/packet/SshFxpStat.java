package it.ftb.sftp.packet;

import it.ftb.sftp.network.Decoder;

public final class SshFxpStat {

    public static final PacketFactory<SshFxpStat> FACTORY = new PacketFactory<SshFxpStat>() {
        @Override
        public void read(Decoder decoder, VoidPacketVisitor visitor) {
            int requestId = decoder.readInt();
            String path = decoder.readString().getString();
            int uFlags = decoder.readInt();
            visitor.visitStat(requestId, path, uFlags);
        }
    };
}
