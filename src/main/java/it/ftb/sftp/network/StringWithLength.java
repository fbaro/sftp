package it.ftb.sftp.network;

public final class StringWithLength {
    private final int length;
    private final String string;

    public StringWithLength(int length, String string) {
        this.length = length;
        this.string = string;
    }

    public String getString() {
        return string;
    }

    public int getLength() {
        return length;
    }
}
