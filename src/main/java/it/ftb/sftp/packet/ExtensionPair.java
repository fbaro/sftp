package it.ftb.sftp.packet;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import it.ftb.sftp.network.Decoder;
import it.ftb.sftp.network.Encoder;
import it.ftb.sftp.network.MalformedPacketException;
import it.ftb.sftp.network.StringWithLength;

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
        Optional<StringWithLength> name = dec.readOptString();
        if (!name.isPresent()) {
            return Optional.empty();
        }
        String data = dec.readString().getString();
        return Optional.of(new ExtensionPair(name.get().getString(), data));
    }

    public static ImmutableList<ExtensionPair> readAll(@Nonnull Decoder dec) {
        ImmutableList.Builder<ExtensionPair> extensions = new ImmutableList.Builder<>();
        do {
            Optional<ExtensionPair> ep = ExtensionPair.read(dec);
            if (ep.isPresent()) {
                extensions.add(ep.get());
            } else {
                break;
            }
        } while (true);
        return extensions.build();
    }

    public static ImmutableList<ExtensionPair> readAll(@Nonnull Decoder dec, int count) {
        ImmutableList.Builder<ExtensionPair> extensions = new ImmutableList.Builder<>();
        for (int i = 0; i < count; i++) {
            extensions.add(ExtensionPair.read(dec).orElseThrow(() -> new MalformedPacketException("Could not find specified extensions")));
        }
        return extensions.build();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("extensionName", extensionName)
                .add("extensionData", extensionData)
                .toString();
    }
}
