package Project;

import Project.packets.Packet;

import java.util.HashMap;

/**
 * Created by Simon Purup Eskildsen on 5/5/17.
 */
public class Event implements Packet {
    private MyTextEvent textEvent;
    private Integer source;
    private HashMap<Integer, Integer> timeStamp;

    public Event(MyTextEvent myTextEvent, Integer source, HashMap<Integer, Integer> timestamp) {
        //The event
        this.textEvent = myTextEvent;
        //The identifier of the source, for now the address of the peer
        this.source = source;
        //The vectorclock of the source at the time of the event.
        this.timeStamp = timestamp;
    }

    public MyTextEvent getTextEvent() {
        return textEvent;
    }

    public Integer getSource() {
        return source;
    }

    public HashMap<Integer, Integer> getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(HashMap<Integer, Integer> timeStamp) {
        this.timeStamp = timeStamp;
    }
}
