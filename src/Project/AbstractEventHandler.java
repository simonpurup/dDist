package Project;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by Simon on 31-05-2017.
 */
public abstract class AbstractEventHandler extends Thread{
    public abstract void handleEvent(Event event);
    public abstract void addConnection(Connection con);

    /** Rearranges the order of the events provided, such that the provided text event is transformed into
     * a resulting (possible set of) text event(s) that updates the current text as if e happened before
     * the provided list of events. It is important that <param>eventsToUndo</param> is ordered in accordance
     * with the order that the events were performed (latest event first, then the second latest and so on).
     * @param eventsToUndo Ordered list of event to undo, latest first
     * @param e The event to be performed before the events in <param>eventsToUndo</param>
     * @return List of events to be performed inorder to carry out the change of the text event <param>e</param>.
     * The list is ordered such that pullFirst() will give the event that is to be carried out first.
     */
    public LinkedList<MyTextEvent> undoTextEvents(ArrayList<MyTextEvent> eventsToUndo, MyTextEvent e){
        int obsDeletions[][] = new int[5][2], nObsDeletions = 0;
        LinkedList<MyTextEvent> eventsToPerform = new LinkedList<>(), tempList = new LinkedList<>();
        eventsToPerform.add(e);
        for(MyTextEvent A : eventsToUndo) {
            MyTextEvent B = eventsToPerform.pollFirst();
            while(B != null) {
                MyTextEvent new_event = null;
                if (A instanceof TextInsertEvent) {
                    if (B instanceof TextInsertEvent) {
                        TextInsertEvent B_tie = (TextInsertEvent) B;
                        if (B.getOffset() < A.getOffset()) {
                            tempList.add(new TextRemoveEvent(A.getOffset(),((TextInsertEvent) A).getText().length()));
                            tempList.add(B);
                            new_event = A;
                        }
                        else {
                            int addition = ((TextInsertEvent) A).getText().length();
                            if(nObsDeletions!=0) {
                                int start=A.getOffset(), end=A.getOffset()+((TextInsertEvent) A).getText().length();
                                for(int i=0;i<nObsDeletions;i++) {
                                    //Make sure that the end is not before beginning of obsRemove and that start is not
                                    //after end of obsRemove
                                    if (start < obsDeletions[i][0] + obsDeletions[i][1] && end > obsDeletions[i][0]) {
                                        if (obsDeletions[i][0] <= start) {
                                            addition -= (obsDeletions[i][0] + obsDeletions[i][1]) - start;
                                            obsDeletions[i][1] -= (obsDeletions[i][0] + obsDeletions[i][1]) - start;
                                        } else {
                                            addition -= end - obsDeletions[i][0];
                                            obsDeletions[i][1] -= end - obsDeletions[i][0];
                                        }
                                    }
                                }
                            }
                            int offset = B.getOffset() + addition;
                            new_event = new TextInsertEvent(offset, B_tie.getText());
                        }
                    } else {
                        if (B.getOffset() >= A.getOffset()) {
                            int offset = B.getOffset() + ((TextInsertEvent) A).getText().length();
                            new_event = new TextRemoveEvent(offset, ((TextRemoveEvent) B).getLength());
                        } else if (B.getOffset() + ((TextRemoveEvent) B).getLength() >= A.getOffset()) {
                            TextRemoveEvent B1 = new TextRemoveEvent(B.getOffset(),A.getOffset()-B.getOffset());
                            tempList.add(B1);
                            new_event = new TextRemoveEvent(B.getOffset(),((TextRemoveEvent) B).getLength()-B1.getLength());
                        } else
                            new_event = B;
                    }
                } else {
                    if (B instanceof TextInsertEvent) {
                        if (B.getOffset() >= A.getOffset()) {
                            if (B.getOffset() >= A.getOffset() + ((TextRemoveEvent) A).getLength()) {
                                int offset = B.getOffset() - ((TextRemoveEvent) A).getLength();
                                new_event = new TextInsertEvent(offset, ((TextInsertEvent) B).getText());
                            } else {
                                obsDeletions[nObsDeletions][0] = A.getOffset();
                                obsDeletions[nObsDeletions][1] = ((TextRemoveEvent) A).getLength();
                                new_event = new TextInsertEvent(A.getOffset(), ((TextInsertEvent) B).getText());
                                nObsDeletions++;
                            }
                        } else
                            new_event = B;
                    } else {
                        if (B.getOffset() >= A.getOffset()) {
                            if (B.getOffset() >= A.getOffset() + ((TextRemoveEvent) A).getLength()) {
                                int offset = B.getOffset() - ((TextRemoveEvent) A).getLength();
                                new_event = new TextRemoveEvent(offset, ((TextRemoveEvent) B).getLength());
                            } else {
                                if (B.getOffset() + ((TextRemoveEvent) B).getLength() >= A.getOffset() + ((TextRemoveEvent) A).getLength()) {
                                    int length = B.getOffset() + ((TextRemoveEvent) B).getLength() - A.getOffset() - ((TextRemoveEvent) A).getLength();
                                    new_event = new TextRemoveEvent(A.getOffset(), length);
                                } else
                                    new_event = null;
                            }
                        } else if (B.getOffset() + ((TextRemoveEvent) B).getLength() >= A.getOffset()) {
                            if (B.getOffset() + ((TextRemoveEvent) B).getLength() >= A.getOffset() + ((TextRemoveEvent) A).getLength()) {
                                int length = ((TextRemoveEvent) B).getLength() - ((TextRemoveEvent) A).getLength();
                                new_event = new TextRemoveEvent(B.getOffset(), length);
                            } else {
                                new_event = new TextRemoveEvent(B.getOffset(), A.getOffset() - B.getOffset());
                            }
                        } else
                            new_event = B;
                    }
                }
                tempList.add(new_event);
                B = eventsToPerform.pollFirst();
            }
            eventsToPerform = tempList;
            tempList = new LinkedList<>();
        }
        return eventsToPerform;
    }
}
