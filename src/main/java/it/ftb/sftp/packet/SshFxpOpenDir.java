package it.ftb.sftp.packet;

import com.google.common.base.MoreObjects;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

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
    public void visit(VoidPacketVisitor visitor) {
        visitor.visitOpenDir(uRequestId, path);
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
