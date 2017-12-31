package it.ftb.sftp.packet;

import com.google.common.collect.ImmutableList;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

import java.util.Optional;

public class SshFxpVersion extends AbstractPacket {

    private final int uVersion;
    private final ImmutableList<ExtensionPair> extensions;

    public SshFxpVersion(int uVersion, ImmutableList<ExtensionPair> extensions) {
        super(PacketType.SSH_FXP_VERSION);
        this.uVersion = uVersion;
        this.extensions = extensions;
    }

    public int getuVersion() {
        return uVersion;
    }

    public ImmutableList<ExtensionPair> getExtensions() {
        return extensions;
    }

    @Override
    public void write(Encoder enc) {
        enc.write(uVersion);
        extensions.forEach(ep -> ep.write(enc));
    }

    @Override
    public <P, R> R visit(P parameter, PacketVisitor<? super P, ? extends R> visitor) {
        return visitor.visit(this, parameter);
    }

    @Override
    public <P> void visit(P parameter, VoidPacketVisitor<? super P> visitor) {
        visitor.visit(this, parameter);
    }

    public static final PacketFactory<SshFxpVersion> FACTORY = new PacketFactory<SshFxpVersion>() {
        @Override
        public SshFxpVersion read(Decoder decoder) {
            int uVersion = decoder.readInt();
            ImmutableList.Builder<ExtensionPair> extensions = new ImmutableList.Builder<>();
            Optional<ExtensionPair> ep;
            while ((ep = ExtensionPair.read(decoder)).isPresent()) {
                extensions.add(ep.get());
            }
            return new SshFxpVersion(uVersion, extensions.build());
        }
    };
}
