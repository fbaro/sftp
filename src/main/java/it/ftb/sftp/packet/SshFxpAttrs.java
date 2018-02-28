package it.ftb.sftp.packet;

import com.google.common.base.MoreObjects;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

public final class SshFxpAttrs extends RequestPacket {

    private final Attrs attrs;

    public SshFxpAttrs(int uRequestId, Attrs attrs) {
        super(PacketType.SSH_FXP_ATTRS, uRequestId);
        this.attrs = attrs;
    }

    public Attrs getAttrs() {
        return attrs;
    }

    @Override
    public void write(Encoder enc) {
        enc.write(uRequestId);
        attrs.write(enc);
    }

    @Override
    public void visit(VoidPacketVisitor visitor) {
        visitor.visitAttrs(uRequestId, attrs);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("attrs", attrs)
                .add("uRequestId", uRequestId)
                .toString();
    }

    public static final PacketFactory<SshFxpAttrs> FACTORY = new PacketFactory<SshFxpAttrs>() {
        @Override
        public void read(Decoder decoder, VoidPacketVisitor visitor) {
            int requestId = decoder.readInt();
            Attrs attrs = Attrs.read(decoder);
            visitor.visitAttrs(requestId, attrs);
        }
    };
}
