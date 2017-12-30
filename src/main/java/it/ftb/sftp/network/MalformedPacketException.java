package it.ftb.sftp.network;

public class MalformedPacketException extends RuntimeException {
    public MalformedPacketException(String message) {
        super(message);
    }
}
