package it.ftb.sftp.packet;

import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum PacketType {

    SSH_FXP_INIT(1, SshFxpInit.FACTORY),
    SSH_FXP_VERSION(2, SshFxpVersion.FACTORY),
    SSH_FXP_OPEN(3, SshFxpOpen.FACTORY),
    SSH_FXP_CLOSE(4, SshFxpClose.FACTORY),
    SSH_FXP_READ(5, SshFxpRead.FACTORY),
    SSH_FXP_WRITE(6, SshFxpWrite.FACTORY),
    SSH_FXP_LSTAT(7, SshFxpLstat.FACTORY),
    SSH_FXP_FSTAT(8, SshFxpFstat.FACTORY),
    SSH_FXP_SETSTAT(9, SshFxpSetstat.FACTORY),
    SSH_FXP_FSETSTAT(10, null),
    SSH_FXP_OPENDIR(11, SshFxpOpenDir.FACTORY),
    SSH_FXP_READDIR(12, SshFxpReadDir.FACTORY),
    SSH_FXP_REMOVE(13, null),
    SSH_FXP_MKDIR(14, null),
    SSH_FXP_RMDIR(15, null),
    SSH_FXP_REALPATH(16, SshFxpRealpath.FACTORY),
    SSH_FXP_STAT(17, SshFxpStat.FACTORY),
    SSH_FXP_RENAME(18, null),
    SSH_FXP_READLINK(19, null),
    SSH_FXP_LINK(21, null),
    SSH_FXP_BLOCK(22, null),
    SSH_FXP_UNBLOCK(23, null),
    SSH_FXP_STATUS(101, SshFxpStatus.FACTORY),
    SSH_FXP_HANDLE(102, SshFxpHandle.FACTORY),
    SSH_FXP_DATA(103, SshFxpData.FACTORY),
    SSH_FXP_NAME(104, SshFxpName.FACTORY),
    SSH_FXP_ATTRS(105, SshFxpAttrs.FACTORY),
    SSH_FXP_EXTENDED(200, null),
    SSH_FXP_EXTENDED_REPLY(201, null);

    private final int code;
    private PacketFactory<?> packetFactory;

    PacketType(int code, PacketFactory<?> packetFactory) {
        this.code = code;
        this.packetFactory = packetFactory;
    }

    public int getCode() {
        return code;
    }

    public PacketFactory<?> getPacketFactory() {
        return packetFactory;
    }

    public static PacketType fromCode(int code) {
        return TYPES_BY_CODE.get(code);
    }

    private static final ImmutableMap<Integer, PacketType> TYPES_BY_CODE =
            ImmutableMap.copyOf(
                    Arrays.stream(PacketType.values())
                            .collect(Collectors.toMap(PacketType::getCode, x -> x)));
}
