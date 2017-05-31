package Project;

import javax.lang.model.type.ArrayType;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Simon Purup Eskildsen on 5/5/17.
 */
public class EventHandler extends AbstractEventHandler{
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
        HashMap<Integer, Integer> vectorClock = dte.getVectorClock();
        //TODO initialize Vectorclocks and identifier so we are able to write without being connected
        if(dte.getIdentifier() <= event.getSource() || event.getTimeStamp().get(dte.getIdentifier()).equals(vectorClock.get(dte.getIdentifier()))){
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
            updateVectorClocks(event.getTimeStamp());
        }
        //If it is our own event send it after all other logic to the peers updating the vectorclock first
        else if(dte.getIdentifier().equals(event.getSource())){
            if(!(vectorClock.get(dte.getIdentifier()) == null))
            vectorClock.put(dte.getIdentifier(), vectorClock.get(dte.getIdentifier()) + 1);
            else
                vectorClock.put(dte.getIdentifier(),0);
            event.setTimeStamp(vectorClock);
            sendEvent(new Event(event.getTextEvent(),dte.getIdentifier(),(HashMap<Integer,Integer>)vectorClock.clone()));
        }
        dte.setVectorClock(vectorClock);
    }

    public void sendEvent(Packet event){
        for(Connection con: connections) {
            con.send(event);
        }
    }

    public synchronized LinkedList<String> getConnections(){
        LinkedList<String> ips = new LinkedList<String>();
        for(Connection c : connections){
            ips.add(c.getIP());
        }
        return ips;
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

    public synchronized void addConnection(Connection con){
        connections.add(con);
    }

    public void updateVectorClocks(HashMap<Integer, Integer> newVectorClock) {
        HashMap<Integer,Integer> vectorClock = dte.getVectorClock();
        for (Object o : newVectorClock.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            if(vectorClock.get(pair.getKey()) == null){
                vectorClock.put((Integer) pair.getKey(), (int) pair.getValue());
            }
            else if (vectorClock.get(pair.getKey()) < (int) pair.getValue()) {
                vectorClock.put((Integer) pair.getKey(), (int) pair.getValue());
            }
        }
        dte.setVectorClock(vectorClock);
    }
}
