package it.ftb.sftp.packet;

import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

import javax.annotation.Nonnull;
import java.util.Optional;

public class Attrs {

    public static final Attrs EMPTY = new Attrs();

    public void write(Encoder enc) {
    }

    public static Attrs read(@Nonnull Decoder dec) {
        return EMPTY;
    }

    public static Optional<Attrs> readOpt(@Nonnull Decoder dec) {
        return Optional.empty();
    }
}
