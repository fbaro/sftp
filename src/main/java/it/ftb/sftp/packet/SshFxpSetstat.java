package it.ftb.sftp.packet;

import it.ftb.sftp.network.Decoder;

public class SshFxpSetstat {

    public static final PacketFactory<SshFxpSetstat> FACTORY = new PacketFactory<SshFxpSetstat>() {
        @Override
        public void read(Decoder decoder, VoidPacketVisitor visitor) {
            int requestId = decoder.readInt();
            String path = decoder.readString().getString();
            Attrs attrs = Attrs.read(decoder);
            visitor.visitSetstat(requestId, path, attrs);
        }
    };
}
