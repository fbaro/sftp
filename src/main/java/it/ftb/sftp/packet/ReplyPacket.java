package it.ftb.sftp.packet;

public abstract class ReplyPacket extends AbstractPacket {
    protected final int uRequestId;

    public ReplyPacket(PacketType type, int uRequestId) {
        super(type);
        this.uRequestId = uRequestId;
    }

    public int getuRequestId() {
        return uRequestId;
    }

}
