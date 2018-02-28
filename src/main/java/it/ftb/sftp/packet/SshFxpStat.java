package it.ftb.sftp.packet;

import com.google.common.base.MoreObjects;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

public final class SshFxpStat extends RequestPacket {

    private final String path;
    private final int uFlags;

    private SshFxpStat(int uRequestId, String path, int uFlags) {
        super(PacketType.SSH_FXP_STAT, uRequestId);
        this.path = path;
        this.uFlags = uFlags;
    }

    public String getPath() {
        return path;
    }

    public int getuFlags() {
        return uFlags;
    }

    @Override
    public void write(Encoder enc) {
        enc.write(uRequestId);
        enc.write(path);
        enc.write(uFlags);
    }

    @Override
    public void visit(VoidPacketVisitor visitor) {
        visitor.visitStat(uRequestId, path, uFlags);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("path", path)
                .add("uFlags", uFlags)
                .add("uRequestId", uRequestId)
                .toString();
    }

    public static final PacketFactory<SshFxpStat> FACTORY = new PacketFactory<SshFxpStat>() {
        @Override
        public SshFxpStat read(Decoder decoder) {
            int requestId = decoder.readInt();
            String path = decoder.readString().getString();
            int uFlags = decoder.readInt();
            return new SshFxpStat(requestId, path, uFlags);
        }
    };
}
