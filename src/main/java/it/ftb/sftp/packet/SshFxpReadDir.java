package it.ftb.sftp.packet;

import it.ftb.sftp.network.Bytes;
import it.ftb.sftp.network.Decoder;

public final class SshFxpReadDir {

    public static final PacketFactory<SshFxpReadDir> FACTORY = new PacketFactory<SshFxpReadDir>() {
        @Override
        public void read(Decoder decoder, VoidPacketVisitor visitor) {
            int requestId = decoder.readInt();
            Bytes handle = decoder.readBytes();
            visitor.visitReadDir(requestId, handle);
        }
    };
}
