package it.ftb.sftp.packet;

import it.ftb.sftp.network.Decoder;

public interface PacketFactory<T extends AbstractPacket> {
    void read(Decoder decoder, VoidPacketVisitor visitor);
}
