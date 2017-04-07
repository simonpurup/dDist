package Project.strategies;

import Project.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

/**
 * Created by lærerPC on 06-04-2017.
 */
public class RemoteEventStrategy implements  EventHandlerStrategy{
    private final Socket socket;
    private final JTextArea area;
    private final Thread listenerThread;
    private ObjectOutputStream outStream;
    private DistributedTextEditor dte;

    public RemoteEventStrategy(Socket socket, JTextArea area, DistributedTextEditor dte){
        this.socket = socket;
        this.area = area;
        this.dte = dte;
        try {
            outStream = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        listenerThread = new Thread(new Runnable() {
            public void run() {
                try {
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    while(!Thread.interrupted()){
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
				                        /* We catch all exceptions, as an uncaught exception would make the
				                         * EDT unwind, which is not healthy.
				                         */
                                    }
                                }
                            });
                        }
                    }
                } catch (IOException e) {
                    if(e instanceof EOFException){
                        dte.disconnect();
                    }
                    else {
                        e.printStackTrace();
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
        listenerThread.start();
    }

    public void handleEvent(MyTextEvent event) {
        try {
            outStream.writeObject(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        listenerThread.interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
