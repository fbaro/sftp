package it.ftb.sftp.packet;

import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

import java.util.Optional;
import java.util.OptionalInt;

public class SshFxpOpen extends RequestPacket {

    private final String filename;
    private final int uDesideredAccess;
    private final int uFlags;
    private final Attrs attrs;

    public SshFxpOpen(int uRequestId, String filename, int uDesideredAccess, int uFlags, Attrs attrs) {
        super(PacketType.SSH_FXP_OPEN, uRequestId);
        this.filename = filename;
        this.uDesideredAccess = uDesideredAccess;
        this.uFlags = uFlags;
        this.attrs = attrs;
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
    public <P, R> R visit(PacketVisitor<? super P, ? extends R> visitor, P parameter) {
        return visitor.visit(this, parameter);
    }

    public static final PacketFactory<SshFxpOpen> FACTORY = new PacketFactory<SshFxpOpen>() {
        @Override
        public SshFxpOpen read(Decoder decoder) {
            int requestId = decoder.readInt();
            String filename = decoder.readString();
            OptionalInt desideredAccess = decoder.readOptInt32();
            OptionalInt flags = decoder.readOptInt32();
            Optional<Attrs> attrs = Attrs.read(decoder);
            return new SshFxpOpen(requestId, filename, desideredAccess.orElse(0), flags.orElse(0), attrs.orElse(Attrs.EMPTY));
        }
    };
}
