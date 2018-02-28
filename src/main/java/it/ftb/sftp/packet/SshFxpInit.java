package it.ftb.sftp.packet;

import com.google.common.base.MoreObjects;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

public class SshFxpInit extends AbstractPacket {

    private final int uVersion;

    public SshFxpInit(int uVersion) {
        super(PacketType.SSH_FXP_INIT);
        this.uVersion = uVersion;
    }

    public int getuVersion() {
        return uVersion;
    }

    @Override
    public void write(Encoder enc) {
        enc.write(uVersion);
    }

    @Override
    public void visit(VoidPacketVisitor visitor) {
        visitor.visitInit(uVersion);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("uVersion", uVersion)
                .toString();
    }

    public static final PacketFactory<SshFxpInit> FACTORY = new PacketFactory<SshFxpInit>() {
        @Override
        public void read(Decoder decoder, VoidPacketVisitor visitor) {
            visitor.visitInit(decoder.readInt());
        }
    };
}
