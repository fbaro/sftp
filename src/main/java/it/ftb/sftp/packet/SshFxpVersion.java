package it.ftb.sftp.packet;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

import java.util.Objects;
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
    public <P> void visit(P parameter, VoidPacketVisitor<? super P> visitor) {
        visitor.visitVersion(uVersion, extensions, parameter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SshFxpVersion that = (SshFxpVersion) o;
        return uVersion == that.uVersion &&
                Objects.equals(extensions, that.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uVersion, extensions);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("uVersion", uVersion)
                .add("extensions", extensions)
                .toString();
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
