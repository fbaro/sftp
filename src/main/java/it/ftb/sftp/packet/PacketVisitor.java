package it.ftb.sftp.packet;

public interface PacketVisitor<P, R> {

    R visit(SshFxpInit packet, P parameter);

    R visit(SshFxpVersion packet, P parameter);

    R visit(SshFxpOpen packet, P parameter);

    R visit(SshFxpHandle packet, P parameter);

    R visit(SshFxpClose packet, P parameter);

    R visit(SshFxpRead packet, P parameter);
}
