package it.ftb.sftp.packet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;
import it.ftb.sftp.network.StringWithLength;

import javax.naming.ldap.Control;
import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

public class SshFxpRealpath extends RequestPacket {

    public enum ControlByte {
        SSH_FXP_REALPATH_NO_CHECK(0),
        SSH_FXP_REALPATH_STAT_IF(1),
        SSH_FXP_REALPATH_STAT_ALWAYS(2);

        private final int code;

        ControlByte(int code) {
            this.code = (byte) code;
        }

        public int getCode() {
            return code;
        }
        public static ControlByte fromCode(int code) {
            return TYPES_BY_CODE.get(code);
        }

        private static final ImmutableMap<Integer, ControlByte> TYPES_BY_CODE =
                ImmutableMap.copyOf(
                        Arrays.stream(ControlByte.values())
                                .collect(Collectors.toMap(ControlByte::getCode, x -> x)));
    }

    private final String originalPath;
    private final ControlByte controlByte;
    private final ImmutableList<String> composePath;

    public SshFxpRealpath(int uRequestId, String originalPath, ControlByte controlByte, ImmutableList<String> composePath) {
        super(PacketType.SSH_FXP_REALPATH, uRequestId);
        this.originalPath = originalPath;
        this.controlByte = controlByte;
        this.composePath = composePath;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public ControlByte getControlByte() {
        return controlByte;
    }

    public ImmutableList<String> getComposePath() {
        return composePath;
    }

    @Override
    public void write(Encoder enc) {
        enc.write(uRequestId);
        enc.write(originalPath);
        if (controlByte == ControlByte.SSH_FXP_REALPATH_NO_CHECK
            && composePath.isEmpty()) {
            return;
        }
        enc.write((byte) controlByte.getCode());
        for (String cp : composePath) {
            enc.write(cp);
        }
    }

    @Override
    public <P, R> R visit(P parameter, PacketVisitor<? super P, ? extends R> visitor) {
        return visitor.visit(this, parameter);
    }

    @Override
    public <P> void visit(P parameter, VoidPacketVisitor<? super P> visitor) {
        visitor.visit(this, parameter);
    }

    public static final PacketFactory<SshFxpRealpath> FACTORY = new PacketFactory<SshFxpRealpath>() {
        @Override
        public SshFxpRealpath read(Decoder decoder) {
            int requestId = decoder.readInt();
            String originalPath = decoder.readString().getString();
            OptionalInt controlByte = decoder.readOptByte();
            ImmutableList.Builder<String> composePath = new ImmutableList.Builder<>();
            Optional<StringWithLength> cp;
            while ((cp = decoder.readOptString()).isPresent()) {
                composePath.add(cp.get().getString());
            }
            return new SshFxpRealpath(requestId, originalPath,
                    ControlByte.fromCode(controlByte.orElse(ControlByte.SSH_FXP_REALPATH_NO_CHECK.getCode())),
                    composePath.build());
        }
    };
}
