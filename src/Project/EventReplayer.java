package Project;

import Project.strategies.EventHandlerStrategy;

import javax.swing.JTextArea;
import java.awt.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * Takes the event recorded by the DocumentEventCapturer and replays
 * them in a JTextArea. The delay of 1 sec is only to make the individual
 * steps in the reply visible to humans.
 *
 * @author Jesper Buus Nielsen
 *
 */
public class EventReplayer implements Runnable {

	private final DistributedTextEditor dte;
	private ArrayList<MyTextEvent> recievedEvents;
	private DocumentEventCapturer dec;
	private JTextArea area;
	private Connection connection;
	private ArrayList<LoggedEvent> eventLog;
	private static final long saveTime = 10^10;

	public EventReplayer(DocumentEventCapturer dec, JTextArea area, DistributedTextEditor dte) {
		this.dec = dec;
		this.area = area;
		recievedEvents = new ArrayList<MyTextEvent>();
		eventLog = new ArrayList<LoggedEvent>();
		this.dte = dte;
	}

	public synchronized  void addRecievedEvent(MyTextEvent e){
		recievedEvents.add(e);
	}

	public synchronized  boolean isRecievedEvent(MyTextEvent e){
		return recievedEvents.remove(e);
	}

	public void run() {
		boolean wasInterrupted = false;
		while (!wasInterrupted) {
			try {
				MyTextEvent mte = dec.take();
				//If event was not recieved it must be a local event.
				if(!isRecievedEvent(mte)){
					if(mte instanceof TextInsertEvent && ((TextInsertEvent) mte).getText() == null) {
						HashMap<String, Integer> vectorClock = dte.getVectorClock();
						eventLog.add(new LoggedEvent(mte, vectorClock, System.nanoTime()));
						while (eventLog.size() > 0 && System.nanoTime() - eventLog.get(0).time > saveTime) {
							eventLog.remove(0);
						}
						vectorClock.put(dte.getLocalAddress(), vectorClock.get(dte.getLocalAddress()) + 1);
						if (connection != null) connection.send(new EventMessage(vectorClock, mte));
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void handleMessage(EventMessage message){
		MyTextEvent mte = message.getTextEvent();
		HashMap<String, Integer> vectorClock = dte.getVectorClock();

		addRecievedEvent(mte);
		eventLog.add(new LoggedEvent(mte,vectorClock, System.nanoTime()));

		printMessage(message.getTextEvent());
	}

	public void printMessage(MyTextEvent mte) {
		if (mte instanceof TextInsertEvent) {
			final TextInsertEvent tie = (TextInsertEvent)mte;
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					try {
						area.insert(tie.getText(), tie.getOffset());
					} catch (Exception e) {
						e.printStackTrace();
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
						e.printStackTrace();
				                        /* We catch all exceptions, as an uncaught exception would make the
				                         * EDT unwind, which is not healthy.
				                         */
					}
				}
			});
		}
	}

	public void connect(Socket socket) {
		connection = new Connection(socket, this);
		new Thread(connection).start();
	}

	public void disConnect() {
		connection.disconnect();
	}
}
