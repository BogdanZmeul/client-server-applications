package data;

public class Package {
    private byte bSrc;
    private long bPktId;
    private Message message;

    public Package(){}

    public Package(byte bSrc, long bPktId, Message message) {
        this.bSrc = bSrc;
        this.bPktId = bPktId;
        this.message = message;
    }

    public byte getbSrc() {
        return bSrc;
    }

    public void setbSrc(byte bSrc) {
        this.bSrc = bSrc;
    }

    public long getbPktId() {
        return bPktId;
    }

    public void setbPktId(long bPktId) {
        this.bPktId = bPktId;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
