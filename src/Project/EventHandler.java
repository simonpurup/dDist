package Project;

import Project.packets.Packet;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/** Created by Simon Purup Eskildsen on 5/5/17.
 */
public class EventHandler extends AbstractEventHandler{
    private LinkedList<Event> eventLog;
    private LinkedList<MyTextEvent> eventsPerformed;
    private LinkedBlockingQueue<Event> eventsToPerform;
    private DistributedTextEditor dte;
    private JTextArea area;
    private LinkedBlockingQueue<Connection> connections;

    public EventHandler(LinkedBlockingQueue<Event> eventsToPerform, DistributedTextEditor dte) {
        this.eventsToPerform = eventsToPerform;
        this.dte = dte;
        area = dte.getArea();
        eventsPerformed = dte.getEventsPerformed();
        connections = new LinkedBlockingQueue<>();
        eventLog = new LinkedList<>();
    }

    @Override
    public void run() {
        while(!isInterrupted()){
            try {
                if(!connections.isEmpty())
                    checkDelayedEvents();
                Event event = eventsToPerform.take();
                handleEvent(event);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void handleEvent(Event event){
        HashMap<Integer, Integer> vectorClock = dte.getVectorClock();

        ArrayList<MyTextEvent> eventsToRollBack = new ArrayList<>();
        HashMap<Integer,Integer> vc_l, vc_e = event.getTimeStamp();
        for(Event e : eventLog){
            vc_l = e.getTimeStamp();
            boolean leq = true;
            for (int id : vc_e.keySet()) {
                if (vc_l.get(id) != null && vc_l.get(id) > vc_e.get(id)) {
                    leq = false;
                }
            }
            if(leq || e.getSource() > event.getSource()){
                break;
            }
            eventsToRollBack.add(e.getTextEvent());
        }
        LinkedList<MyTextEvent> eventsToShow = undoTextEvents(eventsToRollBack,event.getTextEvent());
        for(MyTextEvent mte : eventsToShow){
            eventsPerformed.add(mte);
            executeEvent(mte);
        }

        //Syncs vector-clocks For all x: Local(V[x) = max(Local(V[x]),Message(V[x]))
        if(!dte.getIdentifier().equals(event.getSource())) {
            eventLog.add(eventsToRollBack.size(),event);
            updateVectorClocks(event.getTimeStamp());
        }
        //If it is our own event send it after all other logic to the peers updating the vectorClock first
        else{
            if(!(vectorClock.get(dte.getIdentifier()) == null))
                vectorClock.put(dte.getIdentifier(), vectorClock.get(dte.getIdentifier()) + 1);
            else
                vectorClock.put(dte.getIdentifier(), 1);
            event.setTimeStamp((HashMap<Integer, Integer>) vectorClock.clone());
            eventLog.add(eventsToRollBack.size(),event);
            sendEvent(new Event(event.getTextEvent(), dte.getIdentifier(), (HashMap<Integer, Integer>) vectorClock.clone()));
        }
        dte.setVectorClock(vectorClock);
    }

    public void sendEvent(Packet event){
        for(Connection con: connections) {
            con.send(event);
        }
    }

    private void checkDelayedEvents(){
        for(Connection con: connections) {
            con.checkDelayedEvents();
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

    public void disconnect() {
        for(Connection c : connections){
            c.disconnect();
        }
        connections.clear();
    }

    public void removeConnection(Connection connection) {
        connections.remove(connection);
    }
}
