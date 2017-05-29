package dDist.Project;

import Project.EventHandler;
import Project.MyTextEvent;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by Simon on 29-05-2017.
 */
public class EventHandlerStub {
    EventHandler handler;

    public EventHandlerStub() {
        handler = new EventHandler(null,new DistributedTextEditorStub());
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
    public LinkedList<MyTextEvent> undoTextEvents(ArrayList<MyTextEvent> eventsToUndo, MyTextEvent e){
       return handler.undoTextEvents(eventsToUndo,e);
    }
}
