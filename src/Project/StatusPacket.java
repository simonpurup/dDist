package Project;

/**
 * Created by malthe on 30/05/2017.
 */
public class StatusPacket implements Packet {
    private final int identifier;
    private final String text;

    public StatusPacket(String text, int identifier) {
        this.text = text;
        this.identifier = identifier;
    }

    public int getIdentifier() {
        return identifier;
    }

    public String getText() {
        return text;
    }
}
