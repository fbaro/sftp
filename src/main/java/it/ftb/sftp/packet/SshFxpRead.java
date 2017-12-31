package it.ftb.sftp.packet;

import it.ftb.sftp.network.Bytes;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

public class SshFxpRead extends RequestPacket {

    private final Bytes handle;
    private final long uOffset;
    private final int uLength;

    private SshFxpRead(int uRequestId, Bytes handle, long uOffset, int uLength) {
        super(PacketType.SSH_FXP_READ, uRequestId);
        this.handle = handle;
        this.uOffset = uOffset;
        this.uLength = uLength;
    }

    public Bytes getHandle() {
        return handle;
    }

    public long getuOffset() {
        return uOffset;
    }

    public int getuLength() {
        return uLength;
    }

    @Override
    public void write(Encoder enc) {
        enc.write(uRequestId)
                .write(handle)
                .write(uOffset)
                .write(uLength);
    }

    @Override
    public <P, R> R visit(P parameter, PacketVisitor<? super P, ? extends R> visitor) {
        return visitor.visit(this, parameter);
    }

    @Override
    public <P> void visit(P parameter, VoidPacketVisitor<? super P> visitor) {
        visitor.visit(this, parameter);
    }

    public static final PacketFactory<SshFxpRead> FACTORY = new PacketFactory<SshFxpRead>() {
        @Override
        public SshFxpRead read(Decoder decoder) {
            int requestId = decoder.readInt();
            Bytes handle = decoder.readBytes();
            long offset = decoder.readLong();
            int length = decoder.readInt();
            return new SshFxpRead(requestId, handle, offset, length);
        }
    };
}
