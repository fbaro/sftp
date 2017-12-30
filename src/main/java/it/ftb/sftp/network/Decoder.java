package it.ftb.sftp.network;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

public interface Decoder {

    OptionalInt readOptByte();

    OptionalInt readOptInt();

    OptionalLong readOptLong();

    Optional<String> readOptString();

    String readString();

    Bytes readBytes();

    int readInt();

    long readLong();
}
