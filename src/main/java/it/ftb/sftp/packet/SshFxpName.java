package it.ftb.sftp.packet;

import com.google.common.collect.ImmutableList;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

import java.util.Optional;

public class SshFxpName extends ReplyPacket {
    private final ImmutableList<String> names;
    private final ImmutableList<Attrs> attributes;
    private final Optional<Boolean> endOfList;

    SshFxpName(int uRequestId, ImmutableList<String> names, ImmutableList<Attrs> attributes, Optional<Boolean> endOfList) {
        super(PacketType.SSH_FXP_NAME, uRequestId);
        this.names = names;
        this.attributes = attributes;
        this.endOfList = endOfList;
    }

    public ImmutableList<String> getNames() {
        return names;
    }

    public ImmutableList<Attrs> getAttributes() {
        return attributes;
    }

    public Optional<Boolean> getEndOfList() {
        return endOfList;
    }

    @Override
    public void write(Encoder enc) {
        enc.write(uRequestId);
        enc.write(names.size());
        for (int i = 0; i < names.size(); i++) {
            enc.write(names.get(i));
            attributes.get(i).write(enc);
        }
        endOfList.ifPresent(enc::write);
    }

    @Override
    public <P, R> R visit(P parameter, PacketVisitor<? super P, ? extends R> visitor) {
        return visitor.visit(this, parameter);
    }

    @Override
    public <P> void visit(P parameter, VoidPacketVisitor<? super P> visitor) {
        visitor.visit(this, parameter);
    }

    public static final PacketFactory<SshFxpName> FACTORY = new PacketFactory<SshFxpName>() {
        @Override
        public SshFxpName read(Decoder decoder) {
            int uRequestId = decoder.readInt();
            int count = decoder.readInt();
            ImmutableList.Builder<String> names = new ImmutableList.Builder<>();
            ImmutableList.Builder<Attrs> attributes = new ImmutableList.Builder<>();
            for (int i = 0; i < count; i++) {
                names.add(decoder.readString().getString());
                attributes.add(Attrs.read(decoder));
            }
            Optional<Boolean> endOfList = decoder.readOptBoolean();
            return new SshFxpName(uRequestId, names.build(), attributes.build(), endOfList);
        }
    };
}
