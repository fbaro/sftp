package it.ftb.sftp.packet;

public abstract class RequestPacket extends AbstractPacket {

    protected final int uRequestId;

    public RequestPacket(PacketType type, int uRequestId) {
        super(type);
        this.uRequestId = uRequestId;
    }

    public int getuRequestId() {
        return uRequestId;
    }

}
