package it.ftb.sftp.packet;

public interface VoidPacketVisitor<P> {

    void visit(AbstractPacket packet, P parameter);

    default void visit(SshFxpInit packet, P parameter) {
        visit((AbstractPacket) packet, parameter);
    }

    default void visit(SshFxpVersion packet, P parameter) {
        visit((AbstractPacket) packet, parameter);
    }

    default void visit(SshFxpOpen packet, P parameter) {
        visit((AbstractPacket) packet, parameter);
    }

    default void visit(SshFxpOpenDir packet, P parameter) {
        visit((AbstractPacket) packet, parameter);
    }

    default void visit(SshFxpHandle packet, P parameter) {
        visit((AbstractPacket) packet, parameter);
    }

    default void visit(SshFxpClose packet, P parameter) {
        visit((AbstractPacket) packet, parameter);
    }

    default void visit(SshFxpReadDir packet, P parameter) {
        visit((AbstractPacket) packet, parameter);
    }

    default void visit(SshFxpRead packet, P parameter) {
        visit((AbstractPacket) packet, parameter);
    }

    default void visit(SshFxpData packet, P parameter) {
        visit((AbstractPacket) packet, parameter);
    }

    default void visit(SshFxpStatus packet, P parameter) {
        visit((AbstractPacket) packet, parameter);
    }

    default void visit(SshFxpName packet, P parameter) {
        visit((AbstractPacket) packet, parameter);
    }

    default void visit(SshFxpLstat packet, P parameter) {
        visit((AbstractPacket) packet, parameter);
    }

    default void visit(SshFxpStat packet, P parameter) {
        visit((AbstractPacket) packet, parameter);
    }

    default void visit(SshFxpFstat packet, P parameter) {
        visit((AbstractPacket) packet, parameter);
    }

    default void visit(SshFxpRealpath packet, P parameter) {
        visit((AbstractPacket) packet, parameter);
    }

    default void visit(SshFxpAttrs packet, P parameter) {
        visit((AbstractPacket) packet, parameter);
    }

    default void visit(SshFxpWrite packet, P parameter) {
        visit((AbstractPacket) packet, parameter);
    }
}
