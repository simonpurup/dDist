package Project.strategies;

import Project.MyTextEvent;
import Project.TextInsertEvent;
import Project.TextRemoveEvent;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by lærerPC on 06-04-2017.
 */
public class RemoteEventStrategy implements  EventHandlerStrategy{
    private final Socket socket;
    private final JTextArea area;
    private  ObjectOutputStream outStream;
    private boolean closed = false;

    public RemoteEventStrategy(Socket socket, JTextArea area){
        this.socket = socket;
        this.area = area;
        try {
            outStream = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            public void run() {
                try {
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    while(!closed){
                        MyTextEvent mte = (MyTextEvent) inputStream.readObject();
                        if (mte instanceof TextInsertEvent) {
                            final TextInsertEvent tie = (TextInsertEvent)mte;
                            EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                    try {
                                        area.insert(tie.getText(), tie.getOffset());
                                    } catch (Exception e) {
                                        System.err.println(e);
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
                                        System.err.println(e);
				    /* We catch all axceptions, as an uncaught exception would make the
				     * EDT unwind, which is now healthy.
				     */
                                    }
                                }
                            });
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    public void handleEvent(MyTextEvent event) {
        try {
            outStream.writeObject(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
