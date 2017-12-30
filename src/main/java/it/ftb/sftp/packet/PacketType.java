package it.ftb.sftp.packet;

public enum PacketType {

    SSH_FXP_INIT(1, SshFxpInit.FACTORY),
    SSH_FXP_VERSION(2, SshFxpVersion.FACTORY),
    SSH_FXP_OPEN(3, SshFxpOpen.FACTORY),
    SSH_FXP_CLOSE(4, SshFxpClose.FACTORY),
    SSH_FXP_READ(5, SshFxpRead.FACTORY),
    SSH_FXP_WRITE(6, null),
    SSH_FXP_LSTAT(7, null),
    SSH_FXP_FSTAT(8, null),
    SSH_FXP_SETSTAT(9, null),
    SSH_FXP_FSETSTAT(10, null),
    SSH_FXP_OPENDIR(11, null),
    SSH_FXP_READDIR(12, null),
    SSH_FXP_REMOVE(13, null),
    SSH_FXP_MKDIR(14, null),
    SSH_FXP_RMDIR(15, null),
    SSH_FXP_REALPATH(16, null),
    SSH_FXP_STAT(17, null),
    SSH_FXP_RENAME(18, null),
    SSH_FXP_READLINK(19, null),
    SSH_FXP_LINK(21, null),
    SSH_FXP_BLOCK(22, null),
    SSH_FXP_UNBLOCK(23, null),
    SSH_FXP_STATUS(101, null),
    SSH_FXP_HANDLE(102, SshFxpHandle.FACTORY),
    SSH_FXP_DATA(103, null),
    SSH_FXP_NAME(104, null),
    SSH_FXP_ATTRS(105, null),
    SSH_FXP_EXTENDED(200, null),
    SSH_FXP_EXTENDED_REPLY(201, null);

    private final int code;
    private PacketFactory<?> packetFactory;

    private PacketType(int code, PacketFactory<?> packetFactory) {
        this.code = code;
        this.packetFactory = packetFactory;
    }

    public int getCode() {
        return code;
    }

    public PacketFactory<?> getPacketFactory() {
        return packetFactory;
    }
}
