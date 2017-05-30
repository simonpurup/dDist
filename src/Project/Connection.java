package Project;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

public class Connection implements Runnable {

    private final Socket socket;
    private ObjectOutputStream outStream;
    private ObjectInputStream inputStream;
    private LinkedBlockingQueue<Event> eventsToPerform;
    private boolean running;

    public Connection(Socket socket, LinkedBlockingQueue<Event> eventsToPerform) {
        this.socket = socket;
        this.eventsToPerform = eventsToPerform;
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
        return socket.getRemoteSocketAddress().toString();
    }
}
