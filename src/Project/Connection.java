package Project;

import java.awt.*;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by lærerPC on 27-04-2017.
 */
public class Connection implements Runnable {

    private final Socket socket;
    private final EventReplayer er;
    private ObjectOutputStream outStream;
    private ObjectInputStream inputStream;
    private boolean running;

    public Connection(Socket socket, EventReplayer er) {
        this.socket = socket;
        this.er = er;
        try {
            outStream = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            inputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
        }
    }

    //Listens for incomming
    public void run() {
        running = true;
        while (running) {
            try {
                EventMessage message = (EventMessage) inputStream.readObject();
                er.handleMessage(message, socket.getRemoteSocketAddress().toString());
            } catch (IOException e) {
                if (e instanceof EOFException) {
                    running = false;
                    er.disconnectDTE();
                } else if (e instanceof SocketException && e.getMessage().equals("Socket closed")) {
                    if (running) {
                        e.printStackTrace();
                    }
                    else
                        er.disconnectDTE();
                } else
                    e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public  void send(EventMessage message) {
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
}
