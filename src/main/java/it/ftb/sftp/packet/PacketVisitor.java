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

    default R visit(SshFxpOpenDir packet, P parameter) {
        return visit((AbstractPacket) packet, parameter);
    }

    default R visit(SshFxpHandle packet, P parameter) {
        return visit((AbstractPacket) packet, parameter);
    }

    default R visit(SshFxpClose packet, P parameter) {
        return visit((AbstractPacket) packet, parameter);
    }

    default R visit(SshFxpReadDir packet, P parameter) {
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

    default R visit(SshFxpName packet, P parameter) {
        return visit((AbstractPacket) packet, parameter);
    }

    default R visit(SshFxpLstat packet, P parameter) {
        return visit((AbstractPacket) packet, parameter);
    }

    default R visit(SshFxpStat packet, P parameter) {
        return visit((AbstractPacket) packet, parameter);
    }

    default R visit(SshFxpFstat packet, P parameter) {
        return visit((AbstractPacket) packet, parameter);
    }

    default R visit(SshFxpRealpath packet, P parameter) {
        return visit((AbstractPacket) packet, parameter);
    }

    default R visit(SshFxpAttrs packet, P parameter) {
        return visit((AbstractPacket) packet, parameter);
    }

    default R visit(SshFxpWrite packet, P parameter) {
        return visit((AbstractPacket) packet, parameter);
    }
}
