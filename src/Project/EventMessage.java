package Project;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by Simon Purup Eskildsen on 4/27/17.
 */
public class EventMessage implements Serializable {
    private HashMap<String, Integer> vectorClock;
    private MyTextEvent textEvent;

    public EventMessage(HashMap<String, Integer> vectorClock, MyTextEvent textEvent) {
        this.vectorClock = vectorClock;
        this.textEvent = textEvent;
    }

    public HashMap<String, Integer> getVectorClock() {
        return vectorClock;
    }

    public MyTextEvent getTextEvent() {
        return textEvent;
    }
}
