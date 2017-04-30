package Project;

import java.util.HashMap;

/**
 * Created by lærerPC on 27-04-2017.
 */
public class LoggedEvent {
    public int priority;
    public HashMap<String, Integer> vectorClock;
    public MyTextEvent mte;
    public long time;

    public LoggedEvent(MyTextEvent mte, HashMap<String, Integer> vectorClock, long time, int priority){
        this.mte = mte;
        this.vectorClock = (HashMap<String, Integer>) vectorClock.clone();
        this.time = time;
        this.priority = priority;
    }
}
