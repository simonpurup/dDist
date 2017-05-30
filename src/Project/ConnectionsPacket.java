package Project;

import java.util.LinkedList;

/**
 * Created by malthe on 30/05/2017.
 */
public class ConnectionsPacket implements Packet{
    private final LinkedList<String> ips;

    public ConnectionsPacket(LinkedList<String> ips){
        this.ips = ips;
    }

    public LinkedList<String> getIPS(){
        return ips;
    }
}
