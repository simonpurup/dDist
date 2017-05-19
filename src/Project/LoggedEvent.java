package Project;

import java.util.HashMap;

/**
 * Created by lærerPC on 27-04-2017.
 */
public class LoggedEvent {
    public Event event;
    public long time;

    public LoggedEvent(Event event, long time){
        this.event = event;
        this.time = time;
    }
}
