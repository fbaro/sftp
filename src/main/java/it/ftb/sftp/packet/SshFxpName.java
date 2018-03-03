package it.ftb.sftp.packet;

import com.google.common.collect.ImmutableList;
import it.ftb.sftp.network.Decoder;

import java.util.Optional;

public class SshFxpName {

    public static final PacketFactory<SshFxpName> FACTORY = new PacketFactory<SshFxpName>() {
        @Override
        public void read(Decoder decoder, VoidPacketVisitor visitor) {
            int uRequestId = decoder.readInt();
            int count = decoder.readInt();
            ImmutableList.Builder<String> names = new ImmutableList.Builder<>();
            ImmutableList.Builder<Attrs> attributes = new ImmutableList.Builder<>();
            for (int i = 0; i < count; i++) {
                names.add(decoder.readString().getString());
                attributes.add(Attrs.read(decoder));
            }
            Optional<Boolean> endOfList = decoder.readOptBoolean();
            visitor.visitName(uRequestId, names.build(), attributes.build(), endOfList);
        }
    };
}
