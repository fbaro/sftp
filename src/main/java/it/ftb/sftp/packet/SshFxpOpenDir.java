package it.ftb.sftp.packet;

import com.google.common.base.MoreObjects;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

import java.util.Optional;
import java.util.OptionalInt;

public final class SshFxpOpenDir extends RequestPacket {

    private final String path;

    private SshFxpOpenDir(int uRequestId, String path) {
        super(PacketType.SSH_FXP_OPENDIR, uRequestId);
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    @Override
    public void write(Encoder enc) {
        enc.write(uRequestId);
        enc.write(path);
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
                .add("path", path)
                .add("uRequestId", uRequestId)
                .toString();
    }

    public static final PacketFactory<SshFxpOpenDir> FACTORY = new PacketFactory<SshFxpOpenDir>() {
        @Override
        public SshFxpOpenDir read(Decoder decoder) {
            int requestId = decoder.readInt();
            String path = decoder.readString().getString();
            return new SshFxpOpenDir(requestId, path);
        }
    };
}
