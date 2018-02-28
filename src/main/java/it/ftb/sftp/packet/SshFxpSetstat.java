package it.ftb.sftp.packet;

import com.google.common.base.MoreObjects;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

public class SshFxpSetstat extends RequestPacket {

    private final String path;
    private final Attrs attrs;

    private SshFxpSetstat(int uRequestId, String path, Attrs attrs) {
        super(PacketType.SSH_FXP_SETSTAT, uRequestId);
        this.path = path;
        this.attrs = attrs;
    }

    public String getPath() {
        return path;
    }

    public Attrs getAttrs() {
        return attrs;
    }

    @Override
    public void write(Encoder enc) {
        enc.write(uRequestId);
        enc.write(path);
        attrs.write(enc);
    }

    @Override
    public void visit(VoidPacketVisitor visitor) {
        visitor.visitSetstat(uRequestId, path, attrs);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("path", path)
                .add("attrs", attrs)
                .add("uRequestId", uRequestId)
                .toString();
    }

    public static final PacketFactory<SshFxpSetstat> FACTORY = new PacketFactory<SshFxpSetstat>() {
        @Override
        public SshFxpSetstat read(Decoder decoder) {
            int requestId = decoder.readInt();
            String path = decoder.readString().getString();
            Attrs attrs = Attrs.read(decoder);
            return new SshFxpSetstat(requestId, path, attrs);
        }
    };
}
