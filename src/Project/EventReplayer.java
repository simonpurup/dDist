package Project;

import Project.strategies.EventHandlerStrategy;

import javax.swing.JTextArea;
import java.awt.EventQueue;

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

	private DocumentEventCapturer dec;
	private JTextArea area;
	private EventHandlerStrategy strategy;

	public EventReplayer(DocumentEventCapturer dec, JTextArea area, EventHandlerStrategy strategy) {
		this.dec = dec;
		this.area = area;
		this.strategy = strategy;
	}

	public void run() {
		boolean wasInterrupted = false;
		while (!wasInterrupted) {
			try {
				strategy.handleEvent(dec.take());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("I'm the thread running the EventReplayer, now I die!");
	}

	public void changeStrategy(EventHandlerStrategy strategy){
        this.strategy.close();
        this.strategy = strategy;
    }
}
