package it.ftb.sftp.packet;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

public final class SshFxpOpen extends RequestPacket {

    public enum AceMask {
        ACE4_READ_DATA(0x00000001),
        ACE4_LIST_DIRECTORY(0x00000001),
        ACE4_WRITE_DATA(0x00000002),
        ACE4_ADD_FILE(0x00000002),
        ACE4_APPEND_DATA(0x00000004),
        ACE4_ADD_SUBDIRECTORY(0x00000004),
        ACE4_READ_NAMED_ATTRS(0x00000008),
        ACE4_WRITE_NAMED_ATTRS(0x00000010),
        ACE4_EXECUTE(0x00000020),
        ACE4_DELETE_CHILD(0x00000040),
        ACE4_READ_ATTRIBUTES(0x00000080),
        ACE4_WRITE_ATTRIBUTES(0x00000100),
        ACE4_DELETE(0x00010000),
        ACE4_READ_ACL(0x00020000),
        ACE4_WRITE_ACL(0x00040000),
        ACE4_WRITE_OWNER(0x00080000),
        ACE4_SYNCHRONIZE(0x00100000);

        private final int maskBit;

        AceMask(int maskBit) {
            this.maskBit = maskBit;
        }

        public boolean isSet(int aceMask) {
            return (maskBit & aceMask) != 0;
        }

        public int set(int aceMask) {
            return aceMask |= maskBit;
        }

        public int unset(int aceMask) {
            return aceMask ^ ~maskBit;
        }
    }

    public enum OpenFlagsAccessDisposition {
        SSH_FXF_CREATE_NEW(0x00000000),
        SSH_FXF_CREATE_TRUNCATE(0x00000001),
        SSH_FXF_OPEN_EXISTING(0x00000002),
        SSH_FXF_OPEN_OR_CREATE(0x00000003),
        SSH_FXF_TRUNCATE_EXISTING(0x00000004);

        private final int code;

        OpenFlagsAccessDisposition(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static OpenFlagsAccessDisposition fromFlags(int flags) {
            OpenFlagsAccessDisposition ret = TYPES_BY_CODE.get(flags & 0x7);
            if (ret == null) {
                throw new IllegalArgumentException("Unknown code: " + (flags & 0x7));
            }
            return ret;
        }

        private static final ImmutableMap<Integer, OpenFlagsAccessDisposition> TYPES_BY_CODE =
                ImmutableMap.copyOf(
                        Arrays.stream(OpenFlagsAccessDisposition.values())
                                .collect(Collectors.toMap(OpenFlagsAccessDisposition::getCode, x -> x)));
    }

    public enum OpenFlags {
        SSH_FXF_APPEND_DATA(0x00000008),
        SSH_FXF_APPEND_DATA_ATOMIC(0x00000010),
        SSH_FXF_TEXT_MODE(0x00000020),
        SSH_FXF_BLOCK_READ(0x00000040),
        SSH_FXF_BLOCK_WRITE(0x00000080),
        SSH_FXF_BLOCK_DELETE(0x00000100),
        SSH_FXF_BLOCK_ADVISORY(0x00000200),
        SSH_FXF_NOFOLLOW(0x00000400),
        SSH_FXF_DELETE_ON_CLOSE(0x00000800),
        SSH_FXF_ACCESS_AUDIT_ALARM_INFO(0x00001000),
        SSH_FXF_ACCESS_BACKUP(0x00002000),
        SSH_FXF_BACKUP_STREAM(0x00004000),
        SSH_FXF_OVERRIDE_OWNER(0x00008000);

        private final int maskBit;

        OpenFlags(int maskBit) {
            this.maskBit = maskBit;
        }

        public boolean isSet(int aceMask) {
            return (maskBit & aceMask) != 0;
        }

        public int set(int aceMask) {
            return aceMask |= maskBit;
        }

        public int unset(int aceMask) {
            return aceMask ^ ~maskBit;
        }
    }

    private final String filename;
    private final int uDesideredAccess;
    private final int uFlags;
    private final Attrs attrs;

    private SshFxpOpen(int uRequestId, String filename, int uDesideredAccess, int uFlags, Attrs attrs) {
        super(PacketType.SSH_FXP_OPEN, uRequestId);
        this.filename = filename;
        this.uDesideredAccess = uDesideredAccess;
        this.uFlags = uFlags;
        this.attrs = attrs;
    }

    public String getFilename() {
        return filename;
    }

    public int getuDesideredAccess() {
        return uDesideredAccess;
    }

    public int getuFlags() {
        return uFlags;
    }

    public Attrs getAttrs() {
        return attrs;
    }

    @Override
    public void write(Encoder enc) {
        enc.write(uRequestId);
        enc.write(filename);
        enc.write(uDesideredAccess);
        enc.write(uFlags);
        attrs.write(enc);
    }

    @Override
    public void visit(VoidPacketVisitor visitor) {
        visitor.visitOpen(uRequestId, filename, uDesideredAccess, uFlags, attrs);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("filename", filename)
                .add("uDesideredAccess", uDesideredAccess)
                .add("uFlags", uFlags)
                .add("attrs", attrs)
                .add("uRequestId", uRequestId)
                .toString();
    }

    public static final PacketFactory<SshFxpOpen> FACTORY = new PacketFactory<SshFxpOpen>() {
        @Override
        public SshFxpOpen read(Decoder decoder) {
            int requestId = decoder.readInt();
            String filename = decoder.readString().getString();
            OptionalInt desideredAccess = decoder.readOptInt();
            OptionalInt flags = decoder.readOptInt();
            Optional<Attrs> attrs = Attrs.readOpt(decoder);
            return new SshFxpOpen(requestId, filename, desideredAccess.orElse(0), flags.orElse(0), attrs.orElse(Attrs.EMPTY));
        }
    };
}
