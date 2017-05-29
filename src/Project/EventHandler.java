package Project;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Simon Purup Eskildsen on 5/5/17.
 */
public class EventHandler extends Thread{
    private ArrayList<LoggedEvent> eventLog;
    private LinkedList<MyTextEvent> eventsPerformed;
    private LinkedBlockingQueue<Event> eventsToPerform;
    private DistributedTextEditor dte;
    private JTextArea area;
    private ArrayList<Connection> connections;

    public EventHandler(LinkedBlockingQueue<Event> eventsToPerform, DistributedTextEditor dte) {
        super();
        this.eventsToPerform = eventsToPerform;
        this.dte = dte;
        area = dte.getArea();
        eventsPerformed = dte.getEventsPerformed();
        connections = new ArrayList<>();
        eventLog = new ArrayList<>();
    }

    @Override
    public void run() {
        //TODO removing old elements from the logged queue should be added to the loop
        while(!isInterrupted()){
            try {
                Event event = eventsToPerform.take();
                handleEvent(event);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void handleEvent(Event event){
        //TODO: This does not appear to make any sense at all, we perform the event if we have priority 0
        // or the vectorclock of the event has the same value for our process as the process own vectorclock has
        HashMap<Integer, Integer> vectorClock = dte.getVectorClock();
        if(dte.getIdentifier() == 0 || event.getTimeStamp().get(dte.getIdentifier()).equals(vectorClock.get(dte.getIdentifier()))){
            eventsPerformed.add(event.getTextEvent());
            executeEvent(event.getTextEvent());
            eventLog.add(new LoggedEvent(new Event(event.getTextEvent(),event.getSource(),event.getTimeStamp()),
                    System.nanoTime()));
        }
        //If Local(V[me]) > Message(V[me] && Priority(me) < priority(him) rollback until Local(V[me]) == Message(V[him])
        //then print
        else{
            ArrayList<MyTextEvent> before = new ArrayList<>();
            for(int i = eventLog.size() - 1; i >= 0; i--){
                LoggedEvent e = eventLog.get(i);
                if(e.event.getTimeStamp().get(dte.getIdentifier()) > event.getTimeStamp().get(dte.getIdentifier())){
                    before.add(e.event.getTextEvent());
                }
            }

            LinkedList<MyTextEvent> eventsToRollBack = undoTextEvents(before,event.getTextEvent());
            for(MyTextEvent e : eventsToRollBack){
                eventsPerformed.add(e);
                executeEvent(e);
                eventLog.add(new LoggedEvent(new Event(e,event.getSource(),event.getTimeStamp()),
                        System.nanoTime()));
            }
        }
        //Syncs vector-clocks For all x: Local(V[x) = max(Local(V[x]),Message(V[x]))
        //TODO: Should eventually be: Local(V[x) = max(Local(V[x]),Message(V[x])) + 1
        if(!dte.getIdentifier().equals(event.getSource())) {
            System.out.println("");
            for (Object o : event.getTimeStamp().entrySet()) {
                Map.Entry pair = (Map.Entry) o;
                if (vectorClock.get(pair.getKey()) < (int) pair.getValue()) {
                    vectorClock.put((Integer) pair.getKey(), (int) pair.getValue());
                }
            }
        }
        //If it is our own event send it after all other logic to the peers updating the vectorclock first
        else {
            if(!(vectorClock.get(dte.getIdentifier()) == null))
            vectorClock.put(dte.getIdentifier(), vectorClock.get(dte.getIdentifier()) + 1);
            else
                vectorClock.put(dte.getIdentifier(),0);
            event.setTimeStamp(vectorClock);
            sendEvent(new Event(event.getTextEvent(),dte.getIdentifier(),(HashMap<Integer,Integer>)vectorClock.clone()));
        }
        dte.setVectorClock(vectorClock);
    }

    /** Rearranges the order of the events provided, such that the provided text event is transformed into
     * a resulting (possible set of) text event(s) that updates the current text as if e happened before
     * the provided list of events. It is important that <param>eventsToUndo</param> is ordered in accordance
     * with the order that the events were performed (latest event first, then the second latest and so on).
     * @param eventsToUndo Ordered list of event to undo, latest first
     * @param e The event to be performed before the events in <param>eventsToUndo</param>
     * @return List of events to be performed inorder to carry out the change of the text event <param>e</param>.
     * The list is ordered such that pullFirst() will give the event that is to be carried out first.
     */
    private LinkedList<MyTextEvent> undoTextEvents(ArrayList<MyTextEvent> eventsToUndo, MyTextEvent e){
        LinkedList<MyTextEvent> eventsToPerform = new LinkedList<>(), tempList = new LinkedList<>();
        eventsToPerform.add(e);
        for(MyTextEvent A : eventsToUndo) {
            MyTextEvent B = eventsToPerform.pollFirst();
            while(B != null) {
                MyTextEvent new_event = null;
                if (A instanceof TextInsertEvent) {
                    if (B instanceof TextInsertEvent) {
                        TextInsertEvent B_tie = (TextInsertEvent) B;
                        if (B.getOffset() < A.getOffset())
                            new_event = B;
                        else {
                            int offset = B.getOffset() + ((TextInsertEvent) A).getText().length();
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
                                new_event = new TextInsertEvent(A.getOffset(), ((TextInsertEvent) B).getText());
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

    private void sendEvent(Event event){
        for(Connection con: connections) {
            con.send(event);
        }
    }

    private void executeEvent(MyTextEvent textEvent) {
        if (textEvent instanceof TextInsertEvent) {
            final TextInsertEvent tie = (TextInsertEvent)textEvent;
            EventQueue.invokeLater(() -> {
                try {
                    area.insert(tie.getText(), tie.getOffset());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else if (textEvent instanceof TextRemoveEvent) {
            final TextRemoveEvent tre = (TextRemoveEvent)textEvent;
            EventQueue.invokeLater(() -> {
                try {
                    area.replaceRange(null, tre.getOffset(), tre.getOffset()+tre.getLength());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void addConnection(Connection con){
        connections.add(con);
    }
}
