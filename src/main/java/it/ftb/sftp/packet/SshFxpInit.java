package it.ftb.sftp.packet;

import it.ftb.sftp.network.Decoder;

public class SshFxpInit {

    public static final PacketFactory<SshFxpInit> FACTORY = new PacketFactory<SshFxpInit>() {
        @Override
        public void read(Decoder decoder, VoidPacketVisitor visitor) {
            visitor.visitInit(decoder.readInt());
        }
    };
}
