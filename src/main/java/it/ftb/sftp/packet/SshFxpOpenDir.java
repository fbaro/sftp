package it.ftb.sftp.packet;

import it.ftb.sftp.network.Decoder;

public final class SshFxpOpenDir {

    public static final PacketFactory<SshFxpOpenDir> FACTORY = new PacketFactory<SshFxpOpenDir>() {
        @Override
        public void read(Decoder decoder, VoidPacketVisitor visitor) {
            int requestId = decoder.readInt();
            String path = decoder.readString().getString();
            visitor.visitOpenDir(requestId, path);
        }
    };
}
