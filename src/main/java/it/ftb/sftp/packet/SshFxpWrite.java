package it.ftb.sftp.packet;

import com.google.common.base.MoreObjects;
import it.ftb.sftp.network.Bytes;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

public class SshFxpWrite extends RequestPacket {

    private final Bytes handle;
    private final long uOffset;
    private final Bytes data;

    private SshFxpWrite(int uRequestId, Bytes handle, long uOffset, Bytes data) {
        super(PacketType.SSH_FXP_WRITE, uRequestId);
        this.handle = handle;
        this.uOffset = uOffset;
        this.data = data;
    }

    public Bytes getHandle() {
        return handle;
    }

    public long getuOffset() {
        return uOffset;
    }

    public Bytes getData() {
        return data;
    }

    @Override
    public void write(Encoder enc) {
        enc.write(uRequestId)
                .write(handle)
                .write(uOffset)
                .write(data);
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
                .add("handle", handle)
                .add("uOffset", uOffset)
                .add("data", data)
                .add("uRequestId", uRequestId)
                .toString();
    }

    public static final PacketFactory<SshFxpWrite> FACTORY = new PacketFactory<SshFxpWrite>() {
        @Override
        public SshFxpWrite read(Decoder decoder) {
            int requestId = decoder.readInt();
            Bytes handle = decoder.readBytes();
            long offset = decoder.readLong();
            Bytes data = decoder.readBytes();
            return new SshFxpWrite(requestId, handle, offset, data);
        }
    };
}
