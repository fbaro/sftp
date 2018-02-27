package it.ftb.sftp.packet;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "WeakerAccess"})
public class Attrs implements BasicFileAttributes {

    public static final Attrs EMPTY = new Builder(Type.SSH_FILEXFER_TYPE_UNKNOWN, false).build();

    public enum Validity {
        SSH_FILEXFER_ATTR_SIZE(0x00000001),
        SSH_FILEXFER_ATTR_PERMISSIONS(0x00000004),
        SSH_FILEXFER_ATTR_ACCESSTIME(0x00000008),
        SSH_FILEXFER_ATTR_CREATETIME(0x00000010),
        SSH_FILEXFER_ATTR_MODIFYTIME(0x00000020),
        SSH_FILEXFER_ATTR_ACL(0x00000040),
        SSH_FILEXFER_ATTR_OWNERGROUP(0x00000080),
        SSH_FILEXFER_ATTR_SUBSECOND_TIMES(0x00000100),
        SSH_FILEXFER_ATTR_BITS(0x00000200),
        SSH_FILEXFER_ATTR_ALLOCATION_SIZE(0x00000400),
        SSH_FILEXFER_ATTR_TEXT_HINT(0x00000800),
        SSH_FILEXFER_ATTR_MIME_TYPE(0x00001000),
        SSH_FILEXFER_ATTR_LINK_COUNT(0x00002000),
        SSH_FILEXFER_ATTR_UNTRANSLATED_NAME(0x00004000),
        SSH_FILEXFER_ATTR_CTIME(0x00008000),
        SSH_FILEXFER_ATTR_EXTENDED(0x80000000);

        private final int mask;

        Validity(int mask) {
            this.mask = mask;
        }

        public int getMask() {
            return mask;
        }

        public boolean isSet(int validAttributeFlags) {
            return (validAttributeFlags & mask) != 0;
        }
    }

    public enum Type {
        SSH_FILEXFER_TYPE_REGULAR(1),
        SSH_FILEXFER_TYPE_DIRECTORY(2),
        SSH_FILEXFER_TYPE_SYMLINK(3),
        SSH_FILEXFER_TYPE_SPECIAL(4),
        SSH_FILEXFER_TYPE_UNKNOWN(5),
        SSH_FILEXFER_TYPE_SOCKET(6),
        SSH_FILEXFER_TYPE_CHAR_DEVICE(7),
        SSH_FILEXFER_TYPE_BLOCK_DEVICE(8),
        SSH_FILEXFER_TYPE_FIFO(9);

        private final int code;

        Type(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static Type fromCode(int code) {
            return TYPES_BY_CODE.get(code);
        }

        private static final ImmutableMap<Integer, Type> TYPES_BY_CODE =
                ImmutableMap.copyOf(
                        Arrays.stream(Type.values())
                                .collect(Collectors.toMap(Type::getCode, x -> x)));
    }

    public enum Attribute {
        SSH_FILEXFER_ATTR_FLAGS_READONLY(0x00000001),
        SSH_FILEXFER_ATTR_FLAGS_SYSTEM(0x00000002),
        SSH_FILEXFER_ATTR_FLAGS_HIDDEN(0x00000004),
        SSH_FILEXFER_ATTR_FLAGS_CASE_INSENSITIVE(0x00000008),
        SSH_FILEXFER_ATTR_FLAGS_ARCHIVE(0x00000010),
        SSH_FILEXFER_ATTR_FLAGS_ENCRYPTED(0x00000020),
        SSH_FILEXFER_ATTR_FLAGS_COMPRESSED(0x00000040),
        SSH_FILEXFER_ATTR_FLAGS_SPARSE(0x00000080),
        SSH_FILEXFER_ATTR_FLAGS_APPEND_ONLY(0x00000100),
        SSH_FILEXFER_ATTR_FLAGS_IMMUTABLE(0x00000200),
        SSH_FILEXFER_ATTR_FLAGS_SYNC(0x00000400),
        SSH_FILEXFER_ATTR_FLAGS_TRANSLATION_ERR(0x00000800);

        private final int mask;

        Attribute(int mask) {
            this.mask = mask;
        }

        public int getMask() {
            return mask;
        }
    }

    private final int validAttributeFlags;
    private final Type type;
    private final long size;
    private final long allocationSize;
    private final String owner;
    private final String group;
    private final int permissions;
    private final long atime;
    private final int atimeNseconds;
    private final long createtime;
    private final int createtimeNseconds;
    private final long mtime;
    private final int mtimeNseconds;
    private final long ctime;
    private final int ctimeNseconds;
    private final String acl;
    private final int attribBits;
    private final int attribBitsValid;
    private final byte textHint;
    private final String mimeType;
    private final int linkCount;
    private final String untranslatedName;
    private final ImmutableList<ExtensionPair> extensions;

    public Attrs(int validAttributeFlags, Type type, long size, long allocationSize, String owner, String group,
                 int permissions, long atime, int atimeNseconds, long createtime, int createtimeNseconds, long mtime,
                 int mtimeNseconds, long ctime, int ctimeNseconds, String acl, int attribBits, int attribBitsValid,
                 byte textHint, String mimeType, int linkCount, String untranslatedName,
                 ImmutableList<ExtensionPair> extensions) {
        this.validAttributeFlags = validAttributeFlags;
        this.type = type;
        this.size = size;
        this.allocationSize = allocationSize;
        this.owner = owner;
        this.group = group;
        this.permissions = permissions;
        this.atime = atime;
        this.atimeNseconds = atimeNseconds;
        this.createtime = createtime;
        this.createtimeNseconds = createtimeNseconds;
        this.mtime = mtime;
        this.mtimeNseconds = mtimeNseconds;
        this.ctime = ctime;
        this.ctimeNseconds = ctimeNseconds;
        this.acl = acl;
        this.attribBits = attribBits;
        this.attribBitsValid = attribBitsValid;
        this.textHint = textHint;
        this.mimeType = mimeType;
        this.linkCount = linkCount;
        this.untranslatedName = untranslatedName;
        this.extensions = extensions;
    }

    private FileTime getFileTime(Validity validity, long time, int timeNseconds) {
        if (!validity.isSet(validAttributeFlags)) {
            return FileTime.fromMillis(0);
        } else if (!Validity.SSH_FILEXFER_ATTR_SUBSECOND_TIMES.isSet(validAttributeFlags)) {
            return FileTime.from(time, TimeUnit.SECONDS);
        } else {
            return FileTime.fromMillis(time * 1000 + timeNseconds / 1000000);
        }

    }

    @Override
    public FileTime lastModifiedTime() {
        return getFileTime(Validity.SSH_FILEXFER_ATTR_MODIFYTIME, mtime, mtimeNseconds);
    }

    @Override
    public FileTime lastAccessTime() {
        return getFileTime(Validity.SSH_FILEXFER_ATTR_ACCESSTIME, atime, atimeNseconds);
    }

    @Override
    public FileTime creationTime() {
        return getFileTime(Validity.SSH_FILEXFER_ATTR_CREATETIME, createtime, createtimeNseconds);
    }

    @Override
    public boolean isRegularFile() {
        return type == Type.SSH_FILEXFER_TYPE_REGULAR;
    }

    @Override
    public boolean isDirectory() {
        return type == Type.SSH_FILEXFER_TYPE_DIRECTORY;
    }

    @Override
    public boolean isSymbolicLink() {
        return type == Type.SSH_FILEXFER_TYPE_SYMLINK;
    }

    @Override
    public boolean isOther() {
        return type == Type.SSH_FILEXFER_TYPE_SPECIAL;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public Object fileKey() {
        return null;
    }

    public boolean isValid(Validity validity) {
        return validity.isSet(validAttributeFlags);
    }

    private void checkValid(Validity validity) {
        if (!isValid(validity)) {
            throw new IllegalStateException("Attribute is not present");
        }
    }

    public int getValidAttributeFlags() {
        return validAttributeFlags;
    }

    public Type getType() {
        return type;
    }

    public long getSize() {
        checkValid(Validity.SSH_FILEXFER_ATTR_SIZE);
        return size;
    }

    public long getAllocationSize() {
        checkValid(Validity.SSH_FILEXFER_ATTR_ALLOCATION_SIZE);
        return allocationSize;
    }

    public String getOwner() {
        checkValid(Validity.SSH_FILEXFER_ATTR_OWNERGROUP);
        return owner;
    }

    public String getGroup() {
        checkValid(Validity.SSH_FILEXFER_ATTR_OWNERGROUP);
        return group;
    }

    public int getPermissions() {
        checkValid(Validity.SSH_FILEXFER_ATTR_PERMISSIONS);
        return permissions;
    }

    public long getAtime() {
        checkValid(Validity.SSH_FILEXFER_ATTR_ACCESSTIME);
        return atime;
    }

    public int getAtimeNseconds() {
        checkValid(Validity.SSH_FILEXFER_ATTR_ACCESSTIME);
        checkValid(Validity.SSH_FILEXFER_ATTR_SUBSECOND_TIMES);
        return atimeNseconds;
    }

    public long getCreatetime() {
        checkValid(Validity.SSH_FILEXFER_ATTR_CREATETIME);
        return createtime;
    }

    public int getCreatetimeNseconds() {
        checkValid(Validity.SSH_FILEXFER_ATTR_CREATETIME);
        checkValid(Validity.SSH_FILEXFER_ATTR_SUBSECOND_TIMES);
        return createtimeNseconds;
    }

    public long getMtime() {
        checkValid(Validity.SSH_FILEXFER_ATTR_MODIFYTIME);
        return mtime;
    }

    public int getMtimeNseconds() {
        checkValid(Validity.SSH_FILEXFER_ATTR_MODIFYTIME);
        checkValid(Validity.SSH_FILEXFER_ATTR_SUBSECOND_TIMES);
        return mtimeNseconds;
    }

    public long getCtime() {
        checkValid(Validity.SSH_FILEXFER_ATTR_CTIME);
        return ctime;
    }

    public int getCtimeNseconds() {
        checkValid(Validity.SSH_FILEXFER_ATTR_CTIME);
        checkValid(Validity.SSH_FILEXFER_ATTR_SUBSECOND_TIMES);
        return ctimeNseconds;
    }

    public String getAcl() {
        checkValid(Validity.SSH_FILEXFER_ATTR_ACL);
        return acl;
    }

    public int getAttribBits() {
        checkValid(Validity.SSH_FILEXFER_ATTR_BITS);
        return attribBits;
    }

    public int getAttribBitsValid() {
        checkValid(Validity.SSH_FILEXFER_ATTR_BITS);
        return attribBitsValid;
    }

    public byte getTextHint() {
        checkValid(Validity.SSH_FILEXFER_ATTR_TEXT_HINT);
        return textHint;
    }

    public String getMimeType() {
        checkValid(Validity.SSH_FILEXFER_ATTR_MIME_TYPE);
        return mimeType;
    }

    public int getLinkCount() {
        checkValid(Validity.SSH_FILEXFER_ATTR_LINK_COUNT);
        return linkCount;
    }

    public String getUntranslatedName() {
        checkValid(Validity.SSH_FILEXFER_ATTR_UNTRANSLATED_NAME);
        return untranslatedName;
    }

    public ImmutableList<ExtensionPair> getExtensions() {
        return extensions;
    }

    private void when(Validity validity, Runnable runnable) {
        if (isValid(validity)) {
            runnable.run();
        }
    }

    private void whenSub(Validity validity, Runnable runnable) {
        if (isValid(validity) && isValid(Validity.SSH_FILEXFER_ATTR_SUBSECOND_TIMES)) {
            runnable.run();
        }
    }

    public void write(Encoder enc) {
        enc.write(validAttributeFlags);
        enc.write((byte) type.getCode());
        when(Validity.SSH_FILEXFER_ATTR_SIZE, () -> enc.write(size));
        when(Validity.SSH_FILEXFER_ATTR_ALLOCATION_SIZE, () -> enc.write(allocationSize));
        when(Validity.SSH_FILEXFER_ATTR_OWNERGROUP, () -> enc.write(owner));
        when(Validity.SSH_FILEXFER_ATTR_OWNERGROUP, () -> enc.write(group));
        when(Validity.SSH_FILEXFER_ATTR_PERMISSIONS, () -> enc.write(permissions));
        when(Validity.SSH_FILEXFER_ATTR_ACCESSTIME, () -> enc.write(atime));
        whenSub(Validity.SSH_FILEXFER_ATTR_ACCESSTIME, () -> enc.write(atimeNseconds));
        when(Validity.SSH_FILEXFER_ATTR_CREATETIME, () -> enc.write(createtime));
        whenSub(Validity.SSH_FILEXFER_ATTR_CREATETIME, () -> enc.write(createtimeNseconds));
        when(Validity.SSH_FILEXFER_ATTR_MODIFYTIME, () -> enc.write(mtime));
        whenSub(Validity.SSH_FILEXFER_ATTR_MODIFYTIME, () -> enc.write(mtimeNseconds));
        when(Validity.SSH_FILEXFER_ATTR_CTIME, () -> enc.write(ctime));
        whenSub(Validity.SSH_FILEXFER_ATTR_CTIME, () -> enc.write(ctimeNseconds));
        when(Validity.SSH_FILEXFER_ATTR_ACL, () -> enc.write(acl));
        when(Validity.SSH_FILEXFER_ATTR_BITS, () -> enc.write(attribBits));
        when(Validity.SSH_FILEXFER_ATTR_BITS, () -> enc.write(attribBitsValid));
        when(Validity.SSH_FILEXFER_ATTR_TEXT_HINT, () -> enc.write(textHint));
        when(Validity.SSH_FILEXFER_ATTR_MIME_TYPE, () -> enc.write(mimeType));
        when(Validity.SSH_FILEXFER_ATTR_LINK_COUNT, () -> enc.write(linkCount));
        when(Validity.SSH_FILEXFER_ATTR_UNTRANSLATED_NAME, () -> enc.write(untranslatedName));
        when(Validity.SSH_FILEXFER_ATTR_EXTENDED, () -> enc.write(extensions.size()));
        when(Validity.SSH_FILEXFER_ATTR_EXTENDED, () -> extensions.forEach(ep -> ep.write(enc)));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("validAttributeFlags", validAttributeFlags)
                .add("type", type)
                .add("size", size)
                .add("allocationSize", allocationSize)
                .add("owner", owner)
                .add("group", group)
                .add("permissions", permissions)
                .add("atime", atime)
                .add("atimeNseconds", atimeNseconds)
                .add("createtime", createtime)
                .add("createtimeNseconds", createtimeNseconds)
                .add("mtime", mtime)
                .add("mtimeNseconds", mtimeNseconds)
                .add("ctime", ctime)
                .add("ctimeNseconds", ctimeNseconds)
                .add("acl", acl)
                .add("attribBits", attribBits)
                .add("attribBitsValid", attribBitsValid)
                .add("textHint", textHint)
                .add("mimeType", mimeType)
                .add("linkCount", linkCount)
                .add("untranslatedName", untranslatedName)
                .add("extensions", extensions)
                .toString();
    }

    public static Attrs read(@Nonnull Decoder dec) {
        return read(dec, dec.readInt());
    }

    public static Optional<Attrs> readOpt(@Nonnull Decoder dec) {
        OptionalInt validAttributeFlags = dec.readOptInt();
        if (!validAttributeFlags.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(read(dec, validAttributeFlags.getAsInt()));
    }

    private static void when(int validAttributeFlags, Validity validity, Runnable runnable) {
        if (validity.isSet(validAttributeFlags)) {
            runnable.run();
        }
    }

    private static void whenSub(int validAttributeFlags, Validity validity, Runnable ifSubsecond, Runnable ifNotSubsecond) {
        if (validity.isSet(validAttributeFlags)) {
            if (Validity.SSH_FILEXFER_ATTR_SUBSECOND_TIMES.isSet(validAttributeFlags)) {
                ifSubsecond.run();
            } else {
                ifNotSubsecond.run();
            }
        }
    }

    private static Attrs read(Decoder dec, int validAttributeFlags) {
        Type type = Type.fromCode(dec.readByte());
        Builder b = new Builder(type, Validity.SSH_FILEXFER_ATTR_SUBSECOND_TIMES.isSet(validAttributeFlags));
        when(validAttributeFlags, Validity.SSH_FILEXFER_ATTR_SIZE, () -> b.withSize(dec.readLong()));
        when(validAttributeFlags, Validity.SSH_FILEXFER_ATTR_ALLOCATION_SIZE, () -> b.withAllocationSize(dec.readLong()));
        when(validAttributeFlags, Validity.SSH_FILEXFER_ATTR_OWNERGROUP, () -> b.withOwnerGroup(dec.readString().getString(), dec.readString().getString()));
        when(validAttributeFlags, Validity.SSH_FILEXFER_ATTR_PERMISSIONS, () -> b.withPermissions(dec.readInt()));
        whenSub(validAttributeFlags, Validity.SSH_FILEXFER_ATTR_ACCESSTIME,
                () -> b.withAtime(dec.readLong(), dec.readInt()),
                () -> b.withAtime(dec.readLong(), 0));
        whenSub(validAttributeFlags, Validity.SSH_FILEXFER_ATTR_CREATETIME,
                () -> b.withCreatetime(dec.readLong(), dec.readInt()),
                () -> b.withCreatetime(dec.readLong(), 0));
        whenSub(validAttributeFlags, Validity.SSH_FILEXFER_ATTR_MODIFYTIME,
                () -> b.withMtime(dec.readLong(), dec.readInt()),
                () -> b.withMtime(dec.readLong(), 0));
        whenSub(validAttributeFlags, Validity.SSH_FILEXFER_ATTR_CTIME,
                () -> b.withCtime(dec.readLong(), dec.readInt()),
                () -> b.withCtime(dec.readLong(), 0));
        when(validAttributeFlags, Validity.SSH_FILEXFER_ATTR_ACL, () -> b.withAcl(dec.readString().getString()));
        when(validAttributeFlags, Validity.SSH_FILEXFER_ATTR_BITS, () -> b.withAttribBits(dec.readInt(), dec.readInt()));
        when(validAttributeFlags, Validity.SSH_FILEXFER_ATTR_TEXT_HINT, () -> b.withTextHint(dec.readByte()));
        when(validAttributeFlags, Validity.SSH_FILEXFER_ATTR_MIME_TYPE, () -> b.withMimeType(dec.readString().getString()));
        when(validAttributeFlags, Validity.SSH_FILEXFER_ATTR_LINK_COUNT, () -> b.withLinkCount(dec.readInt()));
        when(validAttributeFlags, Validity.SSH_FILEXFER_ATTR_UNTRANSLATED_NAME, () -> b.withUntranslatedName(dec.readString().getString()));
        when(validAttributeFlags, Validity.SSH_FILEXFER_ATTR_EXTENDED, () -> b.withExtensions(ExtensionPair.readAll(dec, dec.readInt())));

        if (b.validAttributeFlags != validAttributeFlags) {
            throw new IllegalStateException(String.format("validAttributeFlags mismatch(received %x, reconstructed %x)", validAttributeFlags, b.validAttributeFlags));
        }
        return b.build();
    }

    @SuppressWarnings("UnusedReturnValue")
    public static final class Builder {

        private int validAttributeFlags;
        private final Type type;
        private long size;
        private long allocationSize;
        private String owner;
        private String group;
        private int permissions;
        private long atime;
        private int atimeNseconds;
        private long createtime;
        private int createtimeNseconds;
        private long mtime;
        private int mtimeNseconds;
        private long ctime;
        private int ctimeNseconds;
        private String acl;
        private int attribBits;
        private int attribBitsValid;
        private byte textHint;
        private String mimeType;
        private int linkCount;
        private String untranslatedName;
        private ImmutableList<ExtensionPair> extensions;

        public Builder(Type type, boolean subsecondTimes) {
            this.type = type;
            validAttributeFlags = (subsecondTimes ? Validity.SSH_FILEXFER_ATTR_SUBSECOND_TIMES.getMask() : 0);
        }

        public Builder withSize(long size) {
            validAttributeFlags |= Validity.SSH_FILEXFER_ATTR_SIZE.getMask();
            this.size = size;
            return this;
        }

        public Builder withAllocationSize(long allocationSize) {
            validAttributeFlags |= Validity.SSH_FILEXFER_ATTR_ALLOCATION_SIZE.getMask();
            this.allocationSize = allocationSize;
            return this;
        }

        public Builder withOwnerGroup(String owner, String group) {
            validAttributeFlags |= Validity.SSH_FILEXFER_ATTR_OWNERGROUP.getMask();
            this.owner = owner;
            this.group = group;
            return this;
        }

        public Builder withPermissions(int permissions) {
            validAttributeFlags |= Validity.SSH_FILEXFER_ATTR_PERMISSIONS.getMask();
            this.permissions = permissions;
            return this;
        }

        public Builder withAtime(long atime, int atimeNseconds) {
            validAttributeFlags |= Validity.SSH_FILEXFER_ATTR_ACCESSTIME.getMask();
            this.atime = atime;
            this.atimeNseconds = atimeNseconds;
            return this;
        }

        public Builder withCreatetime(long createtime, int createtimeNseconds) {
            validAttributeFlags |= Validity.SSH_FILEXFER_ATTR_CREATETIME.getMask();
            this.createtime = createtime;
            this.createtimeNseconds = createtimeNseconds;
            return this;
        }

        public Builder withMtime(long mtime, int mtimeNseconds) {
            validAttributeFlags |= Validity.SSH_FILEXFER_ATTR_MODIFYTIME.getMask();
            this.mtime = mtime;
            this.mtimeNseconds = mtimeNseconds;
            return this;
        }

        public Builder withCtime(long ctime, int ctimeNseconds) {
            validAttributeFlags |= Validity.SSH_FILEXFER_ATTR_CTIME.getMask();
            this.ctime = ctime;
            this.ctimeNseconds = ctimeNseconds;
            return this;
        }

        public Builder withAcl(String acl) {
            validAttributeFlags |= Validity.SSH_FILEXFER_ATTR_ACL.getMask();
            this.acl = acl;
            return this;
        }

        public Builder withAttribBits(int attribBits, int attribBitsValid) {
            this.attribBits = attribBits;
            this.attribBitsValid = attribBitsValid;
            return this;
        }

        public Builder withAttribute(Attribute attribute, boolean set) {
            validAttributeFlags |= Validity.SSH_FILEXFER_ATTR_BITS.getMask();
            this.attribBitsValid |= attribute.getMask();
            if (set) {
                this.attribBits |= attribute.getMask();
            } else {
                this.attribBits &= ~attribute.getMask();
            }
            return this;
        }

        public Builder withTextHint(byte textHint) {
            validAttributeFlags |= Validity.SSH_FILEXFER_ATTR_TEXT_HINT.getMask();
            this.textHint = textHint;
            return this;
        }

        public Builder withMimeType(String mimeType) {
            validAttributeFlags |= Validity.SSH_FILEXFER_ATTR_MIME_TYPE.getMask();
            this.mimeType = mimeType;
            return this;
        }

        public Builder withLinkCount(int linkCount) {
            validAttributeFlags |= Validity.SSH_FILEXFER_ATTR_LINK_COUNT.getMask();
            this.linkCount = linkCount;
            return this;
        }

        public Builder withUntranslatedName(String untranslatedName) {
            validAttributeFlags |= Validity.SSH_FILEXFER_ATTR_UNTRANSLATED_NAME.getMask();
            this.untranslatedName = untranslatedName;
            return this;
        }

        public Builder withExtensions(List<ExtensionPair> extensions) {
            validAttributeFlags |= Validity.SSH_FILEXFER_ATTR_EXTENDED.getMask();
            this.extensions = ImmutableList.copyOf(extensions);
            return this;
        }

        public Attrs build() {
            return new Attrs(validAttributeFlags, type, size, allocationSize, owner, group, permissions,
                    atime, atimeNseconds, createtime, createtimeNseconds, mtime, mtimeNseconds,
                    ctime, ctimeNseconds, acl, attribBits, attribBitsValid, textHint, mimeType,
                    linkCount, untranslatedName, extensions);
        }
    }
}
