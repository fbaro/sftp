package it.ftb.sftp.packet;

import it.ftb.sftp.network.Decoder;

public class SshFxpStatus {

    public static final PacketFactory<SshFxpStatus> FACTORY = new PacketFactory<SshFxpStatus>() {
        @Override
        public void read(Decoder decoder, VoidPacketVisitor visitor) {
            int requestId = decoder.readInt();
            int errorCode = decoder.readInt();
            String errorMessage = decoder.readString().getString();
            String errorMessageLanguage = decoder.readString().getString();
            visitor.visitStatus(requestId, ErrorCode.fromCode(errorCode), errorMessage, errorMessageLanguage);
        }
    };
}
