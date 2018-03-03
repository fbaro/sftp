package it.ftb.sftp.packet;

import it.ftb.sftp.network.Decoder;

public final class SshFxpAttrs {

    public static final PacketFactory<SshFxpAttrs> FACTORY = new PacketFactory<SshFxpAttrs>() {
        @Override
        public void read(Decoder decoder, VoidPacketVisitor visitor) {
            int requestId = decoder.readInt();
            Attrs attrs = Attrs.read(decoder);
            visitor.visitAttrs(requestId, attrs);
        }
    };
}
