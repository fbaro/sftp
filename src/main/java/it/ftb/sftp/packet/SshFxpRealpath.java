package it.ftb.sftp.packet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.StringWithLength;

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

public class SshFxpRealpath {

    public enum ControlByte {
        SSH_FXP_REALPATH_NO_CHECK(1),
        SSH_FXP_REALPATH_STAT_IF(2),
        SSH_FXP_REALPATH_STAT_ALWAYS(3);

        private final int code;

        ControlByte(int code) {
            this.code = (byte) code;
        }

        public int getCode() {
            return code;
        }

        public static ControlByte fromCode(int code) {
            ControlByte ret = TYPES_BY_CODE.get(code);
            if (ret == null) {
                throw new IllegalArgumentException("Unknown code: " + code);
            }
            return ret;
        }

        private static final ImmutableMap<Integer, ControlByte> TYPES_BY_CODE =
                ImmutableMap.copyOf(
                        Arrays.stream(ControlByte.values())
                                .collect(Collectors.toMap(ControlByte::getCode, x -> x)));
    }

    public static final PacketFactory<SshFxpRealpath> FACTORY = new PacketFactory<SshFxpRealpath>() {
        @Override
        public void read(Decoder decoder, VoidPacketVisitor visitor) {
            int requestId = decoder.readInt();
            String originalPath = decoder.readString().getString();
            OptionalInt controlByte = decoder.readOptByte();
            ImmutableList.Builder<String> composePath = new ImmutableList.Builder<>();
            Optional<StringWithLength> cp;
            while ((cp = decoder.readOptString()).isPresent()) {
                composePath.add(cp.get().getString());
            }
            visitor.visitRealpath(requestId, originalPath,
                    ControlByte.fromCode(controlByte.orElse(ControlByte.SSH_FXP_REALPATH_NO_CHECK.getCode())),
                    composePath.build());
        }
    };
}
