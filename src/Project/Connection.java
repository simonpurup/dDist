package Project;

import javax.swing.text.AbstractDocument;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

public class Connection implements Runnable {

    private final Socket socket;
    private final EventHandler eventHandler;
    private final DistributedTextEditor dte;
    private ObjectOutputStream outStream;
    private ObjectInputStream inputStream;
    private LinkedBlockingQueue<Event> eventsToPerform;
    private boolean running;

    public Connection(Socket socket, LinkedBlockingQueue<Event> eventsToPerform, EventHandler eventHandler, DistributedTextEditor dte) {
        this.socket = socket;
        this.dte = dte;
        this.eventsToPerform = eventsToPerform;
        this.eventHandler = eventHandler;
        try {
            outStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(this).start();
    }

    //Listens for incoming events
    public void run() {
        running = true;
        while (running) {
            try {
                Object o =  inputStream.readObject();
                if(o instanceof  Event)
                    eventsToPerform.add((Event) o);
                if(o instanceof  ConnectionsPacket) connectRest(((ConnectionsPacket) o).getIPS());
                if(o instanceof  ShouldListenPacket) dte.startConnectedListener();
                if(o instanceof  RequestConnectionsPacket){
                    LinkedList<String> connectionIPS = eventHandler.getConnections();
                    send(new ConnectionsPacket(connectionIPS));
                }
                if(o instanceof  RequestStatusPacket){
                    int highestIdentifier = Collections.max(dte.getVectorClock().keySet());
                    HashMap<Integer, Integer> newVectorClock = dte.getVectorClock();
                    newVectorClock.put(highestIdentifier + 1, 0);
                    dte.setVectorClock(newVectorClock);
                    send(new StatusPacket(dte.getArea().getText(), highestIdentifier +1));
                    eventHandler.sendEvent(new NewVectorClocksPacket((HashMap<Integer,Integer>)newVectorClock.clone()));
                    send(new ShouldListenPacket());
                }
                if(o instanceof  StatusPacket){
                    StatusPacket packet = (StatusPacket) o;
                    dte.setIdentifier(packet.getIdentifier());
                    ((AbstractDocument)dte.getArea().getDocument()).setDocumentFilter(null);
                    dte.getArea().setText(packet.getText());
                    ((AbstractDocument)dte.getArea().getDocument()).setDocumentFilter(dte.dec);
                }
                if(o instanceof  NewVectorClocksPacket){
                    eventHandler.updateVectorClocks(((NewVectorClocksPacket) o).getVectorClock());
                }
            } catch (IOException e) {
                //TODO: handle closing of connections
                if (e instanceof EOFException) {
                    running = false;
                    //er.disconnectDTE();
                } else if (e instanceof SocketException) {
                    if (running) {
                        running = false;
                        //er.disconnectDTE();
                    }
                    //else
                        //er.disconnectDTE();
                } else
                    e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void connectRest(LinkedList<String> IPS) {
        int i = 0;
        for(String ip : IPS){
            if(ip.equals(dte.getOriginalIP()) && 40499+i == dte.getOriginalPort()) {
                i++;
                continue;
            }
            Socket socket = null;
            try {
                socket = new Socket(ip, 40499+i);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Connection connection = new Connection(socket,eventsToPerform, eventHandler,dte);
            eventHandler.addConnection(connection);
            i++;
            System.out.println(ip);
        }
        send(new RequestStatusPacket());
    }

    public void send(Packet message) {
        try {
            outStream.writeObject(message);
        } catch (IOException e) {
            if(e instanceof SocketException && (e.getMessage().equals("Socket closed")
                    || e.getMessage().equals("Broken pipe (Write failed)"))){
                //If the connection has been closed, then do nothing. The closing of
                //sockets is handled elsewhere.
            } else {
                e.printStackTrace();
            }
        }
    }

    public void disconnect() {
        running = false;
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getIP() {
        String s = socket.getInetAddress().toString();
        s = s.substring(1);
        return s;
    }

}
