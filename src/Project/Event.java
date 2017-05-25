package Project;

import java.util.HashMap;

/**
 * Created by Simon Purup Eskildsen on 5/5/17.
 */
public class Event {
    private MyTextEvent textEvent;
    private String source;
    private HashMap<String, Integer> timestamp;

    public Event(MyTextEvent myTextEvent, String source, HashMap<String, Integer> timestamp) {
        this.textEvent = myTextEvent;
        this.source = source;
        this.timestamp = timestamp;
    }

    public MyTextEvent getTextEvent() {
        return textEvent;
    }

    public String getSource() {
        return source;
    }

    public HashMap<String, Integer> getTimestamp() {
        return timestamp;
    }
}
