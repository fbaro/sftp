package it.ftb.sftp.packet;

public interface PacketVisitor<P, R> {

    R visit(AbstractPacket packet, P parameter);

    default R visit(SshFxpInit packet, P parameter) {
        return visit((AbstractPacket) packet, parameter);
    }

    default R visit(SshFxpVersion packet, P parameter) {
        return visit((AbstractPacket) packet, parameter);
    }

    default R visit(SshFxpOpen packet, P parameter) {
        return visit((AbstractPacket) packet, parameter);
    }

    default R visit(SshFxpHandle packet, P parameter) {
        return visit((AbstractPacket) packet, parameter);
    }

    default R visit(SshFxpClose packet, P parameter) {
        return visit((AbstractPacket) packet, parameter);
    }

    default R visit(SshFxpRead packet, P parameter) {
        return visit((AbstractPacket) packet, parameter);
    }

    default R visit(SshFxpData packet, P parameter) {
        return visit((AbstractPacket) packet, parameter);
    }

    default R visit(SshFxpStatus packet, P parameter) {
        return visit((AbstractPacket) packet, parameter);
    }
}
