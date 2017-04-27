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
        while (!Thread.interrupted()) {
            try {
                EventMessage message = (EventMessage) inputStream.readObject();
                er.handleMessage(message);

            } catch (IOException e) {
                if (e instanceof EOFException) {
                    //If the connection is closed on the other end, an EOFException will be
                    //thrown. Therefore we disconnect on this end as well.

                    //TODO: Disconnect logic
                } else if (e instanceof SocketException && e.getMessage().equals("Socket closed")) {
                    if (!Thread.interrupted()) {
                        e.printStackTrace();
                    }
                    //If the connection has been closed, and the thread is interrupted, the
                    //connection has been closed by this instance of the DTE, so do nothing.

                    //TODO: Disconnect logic
                } else {
                    e.printStackTrace();
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void send(EventMessage message) {
        try {
            outStream.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
