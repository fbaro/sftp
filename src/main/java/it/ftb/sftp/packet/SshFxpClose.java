package it.ftb.sftp.packet;

import it.ftb.sftp.network.Bytes;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

public class SshFxpClose extends RequestPacket {

    private final Bytes handle;

    public SshFxpClose(int uRequestId, Bytes handle) {
        super(PacketType.SSH_FXP_CLOSE, uRequestId);
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
    public <P, R> R visit(P parameter, PacketVisitor<? super P, ? extends R> visitor) {
        return visitor.visit(this, parameter);
    }

    @Override
    public <P> void visit(P parameter, VoidPacketVisitor<? super P> visitor) {
        visitor.visit(this, parameter);
    }

    public static final PacketFactory<SshFxpClose> FACTORY = new PacketFactory<SshFxpClose>() {
        @Override
        public SshFxpClose read(Decoder decoder) {
            int requestId = decoder.readInt();
            Bytes handle = decoder.readBytes();
            return new SshFxpClose(requestId, handle);
        }
    };
}