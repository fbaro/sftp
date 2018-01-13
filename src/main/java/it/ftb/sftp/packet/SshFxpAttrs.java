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
                .add("attrs", attrs)
                .add("uRequestId", uRequestId)
                .toString();
    }

    public static final PacketFactory<SshFxpAttrs> FACTORY = new PacketFactory<SshFxpAttrs>() {
        @Override
        public SshFxpAttrs read(Decoder decoder) {
            int requestId = decoder.readInt();
            Attrs attrs = Attrs.read(decoder);
            return new SshFxpAttrs(requestId, attrs);
        }
    };
}
