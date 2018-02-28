package it.ftb.sftp.packet;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

import java.util.Optional;

public class SshFxpName extends ReplyPacket {
    private final ImmutableList<String> names;
    private final ImmutableList<Attrs> attributes;
    private final Optional<Boolean> endOfList;

    public SshFxpName(int uRequestId, ImmutableList<String> names, ImmutableList<Attrs> attributes, boolean endOfList) {
        this(uRequestId, names, attributes, Optional.of(endOfList));
    }

    public SshFxpName(int uRequestId, ImmutableList<String> names, ImmutableList<Attrs> attributes, Optional<Boolean> endOfList) {
        super(PacketType.SSH_FXP_NAME, uRequestId);
        Preconditions.checkArgument(names.size() == attributes.size());
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
    public void visit(VoidPacketVisitor visitor) {
        visitor.visitName(uRequestId, names, attributes, endOfList);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("names", names)
                .add("attributes", attributes)
                .add("endOfList", endOfList)
                .add("uRequestId", uRequestId)
                .toString();
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
