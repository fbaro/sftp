package it.ftb.sftp.packet;

import com.google.common.base.MoreObjects;
import it.ftb.sftp.network.Bytes;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

public final class SshFxpReadDir extends RequestPacket {

    private final Bytes handle;

    private SshFxpReadDir(int uRequestId, Bytes handle) {
        super(PacketType.SSH_FXP_READDIR, uRequestId);
        this.handle = handle;
    }

    public Bytes getHandle() {
        return handle;
    }

    @Override
    public void write(Encoder enc) {
        enc.write(uRequestId);
        enc.write(handle);
    }

    @Override
    public void visit(VoidPacketVisitor visitor) {
        visitor.visitReadDir(uRequestId, handle);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("handle", handle)
                .add("uRequestId", uRequestId)
                .toString();
    }

    public static final PacketFactory<SshFxpReadDir> FACTORY = new PacketFactory<SshFxpReadDir>() {
        @Override
        public void read(Decoder decoder, VoidPacketVisitor visitor) {
            int requestId = decoder.readInt();
            Bytes handle = decoder.readBytes();
            visitor.visitReadDir(requestId, handle);
        }
    };
}
