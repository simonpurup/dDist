package dDist.Project;

import Project.MyTextEvent;
import Project.TextInsertEvent;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedList;

public class TestUndoTextEvents {
    private ArrayList<MyTextEvent> eventsToUndo;
    private EventHandlerStub handler = new EventHandlerStub();

    @Before
    public void init(){
        eventsToUndo = new ArrayList<>();
    }

    @Test
    public void shouldBe_ab(){
        eventsToUndo.add(new TextInsertEvent(0,"a"));
        MyTextEvent eventToPerform = new TextInsertEvent(0,"b");
        LinkedList<MyTextEvent> events = handler.undoTextEvents(eventsToUndo,eventToPerform);
        assertEquals(events.size(),1);
        assertEquals(events.peek() instanceof TextInsertEvent,true);
        TextInsertEvent event = (TextInsertEvent) events.poll();
        assertEquals(event.getOffset(),1);
        assertEquals(event.getText(),"b");
    }

    @Test
    public void shouldBe_aaab(){
        eventsToUndo.add(new TextInsertEvent(0,"aaa"));
        MyTextEvent eventToPerform = new TextInsertEvent(0,"b");
        LinkedList<MyTextEvent> events = handler.undoTextEvents(eventsToUndo,eventToPerform);
        assertEquals(events.size(),1);
        assertEquals(events.peek() instanceof TextInsertEvent,true);
        TextInsertEvent event = (TextInsertEvent) events.poll();
        assertEquals(event.getOffset(),3);
        assertEquals(event.getText(),"b");
    }
}
