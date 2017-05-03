package Project;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.util.HashMap;

/**
 * Created by lærerPC on 27-04-2017.
 */
public class LoggedEvent {
    private String text;
    public int priority;
    public HashMap<String, Integer> vectorClock;
    public MyTextEvent mte;
    public long time;

    public LoggedEvent(MyTextEvent mte, HashMap<String, Integer> vectorClock, long time, int priority, JTextArea area){
        if(mte instanceof TextRemoveEvent){
            try {
                this.text = area.getText(mte.getOffset(),((TextRemoveEvent) mte).getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
        this.mte = mte;
        this.vectorClock = (HashMap<String, Integer>) vectorClock.clone();
        this.time = time;
        this.priority = priority;
    }

    public void undoThis(JTextArea area, EventReplayer er) {

        if (mte instanceof TextInsertEvent) {
            TextRemoveEvent tre = new TextRemoveEvent(mte.getOffset(),((TextInsertEvent) mte).getText().length());
            er.printMessage(tre);
            er.addReceivedEvent(tre);
        } else if (mte instanceof TextRemoveEvent) {
            TextInsertEvent tie = new TextInsertEvent(mte.getOffset(), text);
            er.printMessage(tie);
            er.addReceivedEvent(tie);
        }
    }
}
