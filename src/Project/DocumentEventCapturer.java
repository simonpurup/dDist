package Project;

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * This class captures and remembers the text events of the given document on
 * which it is put as a filter. Normally a filter is used to put restrictions
 * on what can be written in a buffer. In out case we just use it to see all
 * the events and make a copy. 
 *
 * @author Jesper Buus Nielsen
 */
public class DocumentEventCapturer extends DocumentFilter {
	private LinkedList<MyTextEvent> eventsPerformed;
	private LinkedBlockingQueue<Event> eventsToPerform;
	private DistributedTextEditor dte;
		/*
         * We are using a blocking queue for two reasons: 
         * 1) They are thread safe, i.e., we can have two threads add and take elements 
         *    at the same time without any race conditions, so we do not have to do  
         *    explicit synchronization.
         * 2) It gives us a member take() which is blocking, i.e., if the queue is
         *    empty, then take() will wait until new elements arrive, which is what
         *    we want, as we then don't need to keep asking until there are new elements.
         */

	public DocumentEventCapturer(LinkedBlockingQueue<Event> eventsToPerform,
								 DistributedTextEditor dte) {
		this.eventsToPerform = eventsToPerform;
		this.dte = dte;
		eventsPerformed = dte.getEventsPerformed();
	}

	/**
	 * If the queue is empty, then the call will block until an element arrives.
	 * If the thread gets interrupted while waiting, we throw InterruptedException.
	 *
	 * @return Head of the recorded event queue.
	 */

	public void insertString(FilterBypass fb, int offset, String str, AttributeSet a)
			throws BadLocationException {
	/* Queue a copy of the event and then modify the textarea */

		if(str != null) { //If the string is zero, we do not consider it an event
			MyTextEvent textEvent = new TextInsertEvent(offset, str);
			//When an action is intercepted it should always be the first element of
			//the eventsPerformed queue if actually want to perfom it
			//If not the event is sent to the eventhandler for consideration
			if(eventsPerformed.peekFirst() != null &&
					eventsPerformed.peekFirst().equals(textEvent)){
				eventsPerformed.remove(textEvent);
				super.insertString(fb, offset, str, a);
			} else {
				eventsToPerform.add(new Event(textEvent,
						dte.getIdentifier(),
						dte.getVectorClock()));
			}
		}

	}

	public void remove(FilterBypass fb, int offset, int length)
			throws BadLocationException {
	/* Queue a copy of the event and then modify the textarea */
		MyTextEvent textEvent = new TextRemoveEvent(offset, length);
		if(eventsPerformed.peekFirst() != null &&
				eventsPerformed.peekFirst().equals(textEvent)){
			eventsPerformed.remove(textEvent);
			super.remove(fb, offset, length);
		} else {
			eventsToPerform.add(new Event(textEvent,
					dte.getIdentifier(),
					dte.getVectorClock()));
		}
	}

	public void replace(FilterBypass fb, int offset, int length, String str, AttributeSet a)
			throws BadLocationException {
	/* Queue a copy of the event and then modify the text */
		if (length > 0) {
			MyTextEvent textEvent = new TextRemoveEvent(offset, length);
			if(eventsPerformed.peekFirst() != null &&
					eventsPerformed.peekFirst().equals(textEvent)){
				eventsPerformed.remove(textEvent);
				super.insertString(fb, offset, str, a);
			} else {
				eventsToPerform.add(new Event(textEvent,
						dte.getIdentifier(),
						dte.getVectorClock()));
			}
		}
		if(str!=null) {
			TextInsertEvent textEvent = new TextInsertEvent(offset, str);
			if(eventsPerformed.peekFirst() != null &&
					eventsPerformed.peekFirst().equals(textEvent)){
				eventsPerformed.remove(textEvent);
				super.insertString(fb, offset, str, a);
			} else {
				eventsToPerform.add(new Event(textEvent,
						dte.getIdentifier(),
						dte.getVectorClock()));
			}
		}
	}
}
