package Project;

import javax.swing.JTextArea;
import java.awt.*;
import java.net.Socket;
import java.util.*;

/** The responsibility of this class is to take text events from the <code>DocumentEventCapturer</code>,
 * decide what to do with them, and do it. It also handles text events received from connected peers.
 * @author Hold 8
 */
public class EventReplayer implements Runnable {

	private final DistributedTextEditor dte;
	private ArrayList<MyTextEvent> receivedEvents;
	private DocumentEventCapturer dec;
	private JTextArea area;
	private Connection connection;
	private ArrayList<LoggedEvent> eventLog;
	private static final long saveTime = (long) Math.pow(10,10);

	public EventReplayer(DocumentEventCapturer dec, JTextArea area, DistributedTextEditor dte) {
		this.dec = dec;
		this.area = area;
		receivedEvents = new ArrayList<>();
		eventLog = new ArrayList<>();
		this.dte = dte;
	}

	/** The run method takes events from the DocumentEventCapturer and determines if they are local
	 * events or received event. If it is not a received event, it adds the event to the eventLog and
	 * then sends it as a message to the other peers.
	 */
	public void run() {
		boolean wasInterrupted = false;
		while (!wasInterrupted) {
			try {
				MyTextEvent mte = dec.take();
				//If event was not received it must be a local event.
				if(!isReceivedEvent(mte)){
					if(!(mte instanceof TextInsertEvent && ((TextInsertEvent) mte).getText() == null)) {
						HashMap<String, Integer> vectorClock = dte.getVectorClock();
							if(vectorClock.get(dte.getLocalAddress()) != null)
						vectorClock.put(dte.getLocalAddress(), vectorClock.get(dte.getLocalAddress()) + 1);
						eventLog.add(new LoggedEvent(mte, vectorClock, System.nanoTime(),dte.priority));
						while (eventLog.size() > 0 && System.nanoTime() - eventLog.get(0).time > saveTime) {
							eventLog.remove(0);
						}
						if (connection != null){
							connection.send(new EventMessage((HashMap<String,Integer>)vectorClock.clone(), mte));
						}
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void handleMessage(EventMessage message, String sender){
		MyTextEvent mte = message.getTextEvent();
		HashMap<String, Integer> vectorClock = dte.getVectorClock();

		addReceivedEvent(mte);

		//For debugging
//		System.out.println("Message");
//		printMap((HashMap<String,Integer>)message.getVectorClock().clone());
//		System.out.println("Local");
//		printMap((HashMap<String,Integer>)vectorClock.clone());

		//If the timestamp of the message corresponding to this process is equal to the actual clock, update
		//local(V[me]) == Message(V[me]) update
		if(message.getVectorClock().get(dte.getLocalAddress()).equals(vectorClock.get(dte.getLocalAddress()))){
			printMessage(message.getTextEvent());
		}
		//If local(V[me]) > Message(V[me] && Priority(me) > priority(him) update
		else{
			//If local(V[me]) > Message(V[me] && Priority(me) > priority(him) update
			if(dte.priority == 0){
				printMessage(message.getTextEvent());
			}
			//If Local(V[me]) > Message(V[me] && Priority(me) < priority(him) rollback until Local(V[me]) == Message(V[him])
			//then print
			else{
				ArrayList<MyTextEvent> before = new ArrayList<>();
				for(int i = eventLog.size() - 1; i >= 0; i--){
					LoggedEvent e = eventLog.get(i);
					if(e.vectorClock.get(dte.getLocalAddress()) > message.getVectorClock().get(dte.getLocalAddress())){
						before.add(e.mte);
					}
				}
				LinkedList<MyTextEvent> eventsToPerform = rearrangeTextEvent(before,mte);
				for(MyTextEvent e : eventsToPerform){
					receivedEvents.remove(mte);
					addReceivedEvent(e);
					printMessage(e);
				}
			}
		}

		//Syncs vector-clocks
		Iterator it = message.getVectorClock().entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			if (pair.getKey().equals(dte.getLocalAddress())) {
			} else if (vectorClock.get(pair.getKey()) < (int) pair.getValue()) {
				vectorClock.put((String) pair.getKey(), (int) pair.getValue());
			}
		}

		//eventLog.add(new LoggedEvent(mte,vectorClock, System.nanoTime(),priority));
	}

	private static void printMap(Map mp) {
		Iterator it = mp.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			System.out.println(pair.getKey() + " = " + pair.getValue());
			it.remove(); // avoids a ConcurrentModificationException
		}
	}

	/** Rearranges the order of the events provided, such that the provided text event is transformed into
	 * a resulting (possible set of) text event(s) that updates the current text as if e happened before
	 * the provided list of events. It is important that <param>eventsToUndo</param> is ordered in accordance
	 * with the order that the events were performed (latest event first, then the second latest and so on).
	 * @param eventsToUndo Ordered list of event to undo, latest first
	 * @param e The event to be performed before the events in <param>eventsToUndo</param>
     * @return List of events to be performed inorder to carry out the change of the text event <param>e</param>.
	 * The list is ordered such that pullFirst() will give the event that is to be carried out first.
     */
	private LinkedList<MyTextEvent> rearrangeTextEvent(ArrayList<MyTextEvent> eventsToUndo, MyTextEvent e){
		LinkedList<MyTextEvent> eventsToPerform = new LinkedList<>(), tempList = new LinkedList<>();
		eventsToPerform.add(e);
		for(MyTextEvent A : eventsToUndo) {
			MyTextEvent B = eventsToPerform.pollFirst();
			while(B != null) {
				MyTextEvent new_event = null;
				if (A instanceof TextInsertEvent) {
					if (B instanceof TextInsertEvent) {
						TextInsertEvent B_tie = (TextInsertEvent) B;
						if (B.getOffset() > A.getOffset())
							new_event = B;
						else {
							int offset = B.getOffset() + ((TextInsertEvent) A).getText().length();
							new_event = new TextInsertEvent(offset, B_tie.getText());
						}
					} else {
						if (B.getOffset() >= A.getOffset()) {
							int offset = B.getOffset() + ((TextInsertEvent) A).getText().length();
							new_event = new TextRemoveEvent(offset, ((TextRemoveEvent) B).getLength());
						} else if (B.getOffset() + ((TextRemoveEvent) B).getLength() >= A.getOffset()) {
							TextRemoveEvent B1 = new TextRemoveEvent(B.getOffset(),A.getOffset()-B.getOffset());
							tempList.add(B1);
							new_event = new TextRemoveEvent(B.getOffset(),((TextRemoveEvent) B).getLength()-B1.getLength());
						} else
							new_event = B;
					}
				} else {
					if (B instanceof TextInsertEvent) {
						if (B.getOffset() >= A.getOffset()) {
							if (B.getOffset() >= A.getOffset() + ((TextRemoveEvent) A).getLength()) {
								int offset = B.getOffset() - ((TextRemoveEvent) A).getLength();
								new_event = new TextInsertEvent(offset, ((TextInsertEvent) B).getText());
							} else {
								new_event = new TextInsertEvent(A.getOffset(), ((TextInsertEvent) B).getText());
							}
						} else
							new_event = B;
					} else {
						if (B.getOffset() >= A.getOffset()) {
							if (B.getOffset() >= A.getOffset() + ((TextRemoveEvent) A).getLength()) {
								int offset = B.getOffset() - ((TextRemoveEvent) A).getLength();
								new_event = new TextRemoveEvent(offset, ((TextRemoveEvent) B).getLength());
							} else {
								if (B.getOffset() + ((TextRemoveEvent) B).getLength() >= A.getOffset() + ((TextRemoveEvent) A).getLength()) {
									int length = B.getOffset() + ((TextRemoveEvent) B).getLength() - A.getOffset() - ((TextRemoveEvent) A).getLength();
									new_event = new TextRemoveEvent(A.getOffset(), length);
								} else
									new_event = null;
							}
						} else if (B.getOffset() + ((TextRemoveEvent) B).getLength() >= A.getOffset()) {
							if (B.getOffset() + ((TextRemoveEvent) B).getLength() >= A.getOffset() + ((TextRemoveEvent) A).getLength()) {
								int length = ((TextRemoveEvent) B).getLength() - ((TextRemoveEvent) A).getLength();
								new_event = new TextRemoveEvent(B.getOffset(), length);
							} else {
								new_event = new TextRemoveEvent(B.getOffset(), A.getOffset() - B.getOffset());
							}
						} else
							new_event = B;
					}
				}
				tempList.add(new_event);
				B = eventsToPerform.pollFirst();
			}
			eventsToPerform = (LinkedList<MyTextEvent>) tempList.clone();
		}
		return eventsToPerform;
	}

	/** Updates the local text with the change from the provided text event.
	 * @param mte the event to be performed
     */
	private void printMessage(MyTextEvent mte) {
		if (mte instanceof TextInsertEvent) {
			final TextInsertEvent tie = (TextInsertEvent)mte;
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					try {
						area.insert(tie.getText(), tie.getOffset());
					} catch (Exception e) {
						e.printStackTrace();
						// We catch all exceptions, as an uncaught exception would make the
						//EDT unwind, which is not healthy.
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
						// We catch all exceptions, as an uncaught exception would make the
						//EDT unwind, which is not healthy.
					}
				}
			});
		}
	}

	public void connect(Socket socket) {
		connection = new Connection(socket, this);
		new Thread(connection).start();
	}

	public void disconnect() {
		connection.disconnect();
		connection = null;
	}
	public void disconnectDTE() {
		connection = null;
		dte.disconnectClear();
	}


	private synchronized  void addReceivedEvent(MyTextEvent e){
		receivedEvents.add(e);
	}

	private synchronized  boolean isReceivedEvent(MyTextEvent e){
		return receivedEvents.remove(e);
	}
}