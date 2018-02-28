package it.ftb.sftp.packet;

import com.google.common.base.MoreObjects;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

public class SshFxpStatus extends ReplyPacket {

    private final ErrorCode errorCode;
    private final String errorMessage;
    private final String errorMessageLanguage;

    public SshFxpStatus(int uRequestId, ErrorCode errorCode, String errorMessage, String errorMessageLanguage) {
        super(PacketType.SSH_FXP_STATUS, uRequestId);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.errorMessageLanguage = errorMessageLanguage;
    }

    @Override
    public void write(Encoder enc) {
        enc.write(uRequestId)
                .write(errorCode.getCode())
                .write(errorMessage)
                .write(errorMessageLanguage);
    }

    @Override
    public void visit(VoidPacketVisitor visitor) {
        visitor.visitStatus(uRequestId, errorCode, errorMessage, errorMessageLanguage);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("errorCode", errorCode)
                .add("errorMessage", errorMessage)
                .add("errorMessageLanguage", errorMessageLanguage)
                .add("uRequestId", uRequestId)
                .toString();
    }

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
