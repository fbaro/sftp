package it.ftb.sftp.packet;

import com.google.common.base.MoreObjects;
import it.ftb.sftp.network.Bytes;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

public class SshFxpHandle extends ReplyPacket {

    private final Bytes handle;

    public SshFxpHandle(int uRequestId, Bytes handle) {
        super(PacketType.SSH_FXP_HANDLE, uRequestId);
        this.handle = handle;
    }

    public Bytes getHandle() {
        return handle;
    }

    @Override
    public void write(Encoder enc) {
        enc.write(uRequestId)
                .write(handle);
    }

    @Override
    public <P> void visit(P parameter, VoidPacketVisitor<? super P> visitor) {
        visitor.visitHandle(uRequestId, handle, parameter);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("handle", handle)
                .add("uRequestId", uRequestId)
                .toString();
    }

    public static final PacketFactory<SshFxpHandle> FACTORY = new PacketFactory<SshFxpHandle>() {
        @Override
        public SshFxpHandle read(Decoder decoder) {
            int requestId = decoder.readInt();
            Bytes handle = decoder.readBytes();
            return new SshFxpHandle(requestId, handle);
        }
    };
}
