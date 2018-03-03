package it.ftb.sftp.packet;

import it.ftb.sftp.network.Bytes;
import it.ftb.sftp.network.Decoder;

import java.util.Optional;

public class SshFxpData {

    public static final PacketFactory<SshFxpData> FACTORY = new PacketFactory<SshFxpData>() {
        @Override
        public void read(Decoder decoder, VoidPacketVisitor visitor) {
            int requestId = decoder.readInt();
            Bytes data = decoder.readBytes();
            Optional<Boolean> endOfFile = decoder.readOptBoolean();
            visitor.visitData(requestId, data, endOfFile.orElse(false));
        }
    };
}
