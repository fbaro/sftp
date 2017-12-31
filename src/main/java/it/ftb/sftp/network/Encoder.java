package it.ftb.sftp.network;

public interface Encoder {

    Encoder write(byte b);

    Encoder write(int i);

    Encoder write(String s);

    Encoder write(Bytes b);

    Encoder write(long l);

    Encoder write(boolean b);
}
