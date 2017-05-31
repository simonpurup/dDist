package Project;

import java.util.HashMap;

/**
 * Created by malthe on 30/05/2017.
 */
public class NewVectorClocksPacket implements Packet {
    private final HashMap<Integer, Integer> vectorClock;

    public NewVectorClocksPacket(HashMap<Integer, Integer> vectorClock) {
        this.vectorClock = vectorClock;
    }

    public HashMap<Integer, Integer> getVectorClock() {
        return vectorClock;
    }
}
