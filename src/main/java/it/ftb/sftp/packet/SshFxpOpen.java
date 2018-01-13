package it.ftb.sftp.packet;

import com.google.common.base.MoreObjects;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

import java.util.Optional;
import java.util.OptionalInt;

public final class SshFxpOpen extends RequestPacket {

    private final String filename;
    private final int uDesideredAccess;
    private final int uFlags;
    private final Attrs attrs;

    private SshFxpOpen(int uRequestId, String filename, int uDesideredAccess, int uFlags, Attrs attrs) {
        super(PacketType.SSH_FXP_OPEN, uRequestId);
        this.filename = filename;
        this.uDesideredAccess = uDesideredAccess;
        this.uFlags = uFlags;
        this.attrs = attrs;
    }

    public String getFilename() {
        return filename;
    }

    public int getuDesideredAccess() {
        return uDesideredAccess;
    }

    public int getuFlags() {
        return uFlags;
    }

    public Attrs getAttrs() {
        return attrs;
    }

    @Override
    public void write(Encoder enc) {
        enc.write(uRequestId);
        enc.write(filename);
        enc.write(uDesideredAccess);
        enc.write(uFlags);
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
                .add("filename", filename)
                .add("uDesideredAccess", uDesideredAccess)
                .add("uFlags", uFlags)
                .add("attrs", attrs)
                .add("uRequestId", uRequestId)
                .toString();
    }

    public static final PacketFactory<SshFxpOpen> FACTORY = new PacketFactory<SshFxpOpen>() {
        @Override
        public SshFxpOpen read(Decoder decoder) {
            int requestId = decoder.readInt();
            String filename = decoder.readString().getString();
            OptionalInt desideredAccess = decoder.readOptInt();
            OptionalInt flags = decoder.readOptInt();
            Optional<Attrs> attrs = Attrs.readOpt(decoder);
            return new SshFxpOpen(requestId, filename, desideredAccess.orElse(0), flags.orElse(0), attrs.orElse(Attrs.EMPTY));
        }
    };
}
