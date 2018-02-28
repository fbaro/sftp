package it.ftb.sftp.packet;

import it.ftb.sftp.network.Encoder;

public abstract class AbstractPacket {

    private final PacketType type;

    AbstractPacket(PacketType type) {
        this.type = type;
    }

    public PacketType getType() {
        return type;
    }

    public abstract void write(Encoder enc);

    public abstract void visit(VoidPacketVisitor visitor);
}
