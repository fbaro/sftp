package it.ftb.sftp.packet;

import com.google.common.base.MoreObjects;
import it.ftb.sftp.network.Bytes;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

public final class SshFxpFstat extends RequestPacket {

    private final Bytes handle;
    private final int uFlags;

    private SshFxpFstat(int uRequestId, Bytes handle, int uFlags) {
        super(PacketType.SSH_FXP_FSTAT, uRequestId);
        this.handle = handle;
        this.uFlags = uFlags;
    }

    public Bytes getHandle() {
        return handle;
    }

    public int getuFlags() {
        return uFlags;
    }

    @Override
    public void write(Encoder enc) {
        enc.write(uRequestId);
        enc.write(handle);
        enc.write(uFlags);
    }

    @Override
    public <P> void visit(P parameter, VoidPacketVisitor<? super P> visitor) {
        visitor.visitFstat(uRequestId, handle, uFlags, parameter);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("handle", handle)
                .add("uFlags", uFlags)
                .add("uRequestId", uRequestId)
                .toString();
    }

    public static final PacketFactory<SshFxpFstat> FACTORY = new PacketFactory<SshFxpFstat>() {
        @Override
        public SshFxpFstat read(Decoder decoder) {
            int requestId = decoder.readInt();
            Bytes handle = decoder.readBytes();
            int uFlags = decoder.readInt();
            return new SshFxpFstat(requestId, handle, uFlags);
        }
    };
}
