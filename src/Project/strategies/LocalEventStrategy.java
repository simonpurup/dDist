package Project.strategies;

import Project.MyTextEvent;
import Project.TextInsertEvent;
import Project.TextRemoveEvent;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Simon Purup Eskildsen on 4/6/17.
 */
public class LocalEventStrategy implements EventHandlerStrategy {

    private JTextArea area;

    public LocalEventStrategy(JTextArea area) {
        this.area = area;
    }

    @Override
    public void handleEvent(MyTextEvent mte) {
        if (mte instanceof TextInsertEvent) {
            final TextInsertEvent tie = (TextInsertEvent)mte;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    try {
                        area.insert(tie.getText(), tie.getOffset());
                    } catch (Exception e) {
                        e.printStackTrace();
                        /* We catch all exceptions, as an uncaught exception would make the
				     	* EDT unwind, which is now healthy.
				     	*/
                    }
                }
            });
        } else if (mte instanceof TextRemoveEvent) {
            final TextRemoveEvent tre = (TextRemoveEvent)mte;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    try {
                        area.replaceRange(null, tre.getOffset(), tre.getOffset()+tre.getLength());
                    } catch (Exception e) {
                        e.printStackTrace();
				        /* We catch all axceptions, as an uncaught exception would make the
				        * EDT unwind, which is now healthy.
				        */
                    }
                }
            });
        }
    }
}
