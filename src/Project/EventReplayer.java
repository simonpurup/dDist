package Project;

import Project.strategies.EventHandlerStrategy;

import javax.swing.JTextArea;
import java.awt.*;
import java.util.ArrayList;

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

	private ArrayList<MyTextEvent> recievedEvents;
	private DocumentEventCapturer dec;
	private JTextArea area;
	private EventHandlerStrategy strategy;
	private Connection connection;

	public EventReplayer(DocumentEventCapturer dec, JTextArea area, DistributedTextEditor dte) {
		this.dec = dec;
		this.area = area;
		this.strategy = strategy;
		recievedEvents = new ArrayList<MyTextEvent>();
	}

	public synchronized  void addRecievedEvent(MyTextEvent e){
		recievedEvents.add(e);
	}

	public synchronized  boolean isRecievedEvent(MyTextEvent e){
		if(recievedEvents.contains(e)){
			recievedEvents.remove(e);
			return true;
		}
		else return false;
	}

	public void run() {
		boolean wasInterrupted = false;
		while (!wasInterrupted) {
			try {
				MyTextEvent event = dec.take();
				strategy.handleEvent(event);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void handleMessage(EventMessage message){
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
}
