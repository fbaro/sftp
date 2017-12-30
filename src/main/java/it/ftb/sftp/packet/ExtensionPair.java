package it.ftb.sftp.packet;

import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;

import javax.annotation.Nonnull;
import java.util.Optional;

public class ExtensionPair {

    private final String extensionName;
    private final String extensionData;

    public ExtensionPair(String extensionName, String extensionData) {
        this.extensionName = extensionName;
        this.extensionData = extensionData;
    }

    public String getExtensionName() {
        return extensionName;
    }

    public String getExtensionData() {
        return extensionData;
    }

    public void write(Encoder enc) {
        enc.write(extensionName);
        enc.write(extensionData);
    }

    public static Optional<ExtensionPair> read(@Nonnull Decoder dec) {
        Optional<String> name = dec.readOptString();
        if (!name.isPresent()) {
            return Optional.empty();
        }
        String data = dec.readString();
        return Optional.of(new ExtensionPair(name.get(), data));
    }
}
