package it.ftb.sftp.packet;

import com.google.common.collect.ImmutableList;
import it.ftb.sftp.network.Decoder;

import java.util.Optional;

public class SshFxpVersion {

    public static final PacketFactory<SshFxpVersion> FACTORY = new PacketFactory<SshFxpVersion>() {
        @Override
        public void read(Decoder decoder, VoidPacketVisitor visitor) {
            int uVersion = decoder.readInt();
            ImmutableList.Builder<ExtensionPair> extensions = new ImmutableList.Builder<>();
            Optional<ExtensionPair> ep;
            while ((ep = ExtensionPair.read(decoder)).isPresent()) {
                extensions.add(ep.get());
            }
            visitor.visitVersion(uVersion, extensions.build());
        }
    };
}
