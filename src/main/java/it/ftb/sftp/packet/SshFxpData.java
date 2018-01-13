package it.ftb.sftp.packet;

import com.google.common.base.MoreObjects;
import it.ftb.sftp.network.Bytes;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

import java.util.Optional;

public class SshFxpData extends ReplyPacket {

    private final Bytes data;
    private final boolean endOfFile;

    public SshFxpData(int uRequestId, Bytes data, boolean endOfFile) {
        super(PacketType.SSH_FXP_DATA, uRequestId);
        this.data = data;
        this.endOfFile = endOfFile;
    }

    public Bytes getData() {
        return data;
    }

    public boolean isEndOfFile() {
        return endOfFile;
    }

    @Override
    public void write(Encoder enc) {
        enc.write(uRequestId)
                .write(data);
        if (endOfFile) {
            enc.write(true);
        }
    }

    @Override
    public <P, R> R visit(P parameter, PacketVisitor<? super P, ? extends R> visitor) {
        return visitor.visit(this, parameter);
    }

    @Override
    public <P> void visit(P parameter, VoidPacketVisitor<? super P> visitor) {
        visitor.visit(this, parameter);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("data", data)
                .add("endOfFile", endOfFile)
                .add("uRequestId", uRequestId)
                .toString();
    }

    public static final PacketFactory<SshFxpData> FACTORY = new PacketFactory<SshFxpData>() {
        @Override
        public SshFxpData read(Decoder decoder) {
            int requestId = decoder.readInt();
            Bytes data = decoder.readBytes();
            Optional<Boolean> endOfFile = decoder.readOptBoolean();
            return new SshFxpData(requestId, data, endOfFile.orElse(false));
        }
    };
}
