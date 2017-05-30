package dDist.Project;

import Project.MyTextEvent;
import Project.TextInsertEvent;
import Project.TextRemoveEvent;
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
    //Test single text insert event, single character/single character
    public void shouldBe_ab(){
        eventsToUndo.add(new TextInsertEvent(0,"a"));
        MyTextEvent eventToPerform = new TextInsertEvent(0,"b");
        LinkedList<MyTextEvent> events = handler.undoTextEvents(eventsToUndo,eventToPerform);
        assertEquals(1, events.size());
        assertEquals(true, events.peek() instanceof TextInsertEvent);
        TextInsertEvent event = (TextInsertEvent) events.poll();
        assertEquals(1, event.getOffset());
        assertEquals("b", event.getText());
    }

    @Test
    //Test single text insert event, multiple character/single character
    public void shouldBe_aaab(){
        eventsToUndo.add(new TextInsertEvent(0,"aaa"));
        MyTextEvent eventToPerform = new TextInsertEvent(0,"b");
        LinkedList<MyTextEvent> events = handler.undoTextEvents(eventsToUndo,eventToPerform);
        assertEquals(1, events.size());
        assertEquals(true, events.peek() instanceof TextInsertEvent);
        TextInsertEvent event = (TextInsertEvent) events.poll();
        assertEquals(3, event.getOffset());
        assertEquals("b", event.getText());
    }

    @Test
    //Test single text insert event, multiple character/multiple character
    public void shouldBe_aaabbb(){
        eventsToUndo.add(new TextInsertEvent(0,"aaa"));
        MyTextEvent eventToPerform = new TextInsertEvent(0,"bbb");
        LinkedList<MyTextEvent> events = handler.undoTextEvents(eventsToUndo,eventToPerform);
        assertEquals(1, events.size());
        assertEquals(true, events.peek() instanceof TextInsertEvent);
        TextInsertEvent event = (TextInsertEvent) events.poll();
        assertEquals(3, event.getOffset());
        assertEquals("bbb", event.getText());
    }

    @Test
    //Test single text insert event, with offset larger than text insert event
    //assume test text is xxx...
    public void shouldBe_bxa(){
        eventsToUndo.add(new TextInsertEvent(1,"a"));
        MyTextEvent eventToPerform = new TextInsertEvent(0,"b");
        LinkedList<MyTextEvent> events = handler.undoTextEvents(eventsToUndo,eventToPerform);
        assertEquals(1, events.size());
        assertEquals(true, events.peek() instanceof TextInsertEvent);
        TextInsertEvent event = (TextInsertEvent) events.poll();
        assertEquals(0, event.getOffset());
        assertEquals("b", event.getText());
    }

    @Test
    //Test two text insert event
    public void shouldBe_abc(){
        eventsToUndo.add(new TextInsertEvent(0,"a"));
        eventsToUndo.add(new TextInsertEvent(1,"b"));
        MyTextEvent eventToPerform = new TextInsertEvent(0,"c");
        LinkedList<MyTextEvent> events = handler.undoTextEvents(eventsToUndo,eventToPerform);
        assertEquals(1, events.size());
        assertEquals(true, events.peek() instanceof TextInsertEvent);
        TextInsertEvent event = (TextInsertEvent) events.poll();
        assertEquals(2, event.getOffset());
        assertEquals("c", event.getText());
    }


    @Test
    //Test text remove event, with offset+length lower than insert event offset
    //(assume text test is xxxxxxxxx...)
    public void shouldBe_xf(){
        eventsToUndo.add(new TextRemoveEvent(1,2));
        MyTextEvent eventToPerform = new TextInsertEvent(3,"f");
        LinkedList<MyTextEvent> events = handler.undoTextEvents(eventsToUndo,eventToPerform);
        assertEquals(1, events.size());
        assertEquals(true, events.peek() instanceof TextInsertEvent);
        TextInsertEvent event = (TextInsertEvent) events.poll();
        assertEquals(1, event.getOffset());
        assertEquals("f", event.getText());
    }

    @Test
    //Test text remove event, with offset lower than insert event offset
    //and offset+length greater than event offset
    public void shouldBe_xxf(){
        eventsToUndo.add(new TextRemoveEvent(2,2));
        MyTextEvent eventToPerform = new TextInsertEvent(3,"f");
        LinkedList<MyTextEvent> events = handler.undoTextEvents(eventsToUndo,eventToPerform);
        assertEquals(1, events.size());
        assertEquals(true, events.peek() instanceof TextInsertEvent);
        TextInsertEvent event = (TextInsertEvent) events.poll();
        assertEquals(2, event.getOffset());
        assertEquals("f",event.getText());
    }

    @Test
    //Test text remove event, with offset larger than insert event offset
    public void shouldBe_xfxx(){
        eventsToUndo.add(new TextRemoveEvent(2,2));
        MyTextEvent eventToPerform = new TextInsertEvent(1,"f");
        LinkedList<MyTextEvent> events = handler.undoTextEvents(eventsToUndo,eventToPerform);
        assertEquals(1, events.size());
        assertEquals(true, events.peek() instanceof TextInsertEvent);
        TextInsertEvent event = (TextInsertEvent) events.poll();
        assertEquals(1, event.getOffset());
        assertEquals("f",event.getText());
    }

    @Test
    //Test text insert event followed by text remove event
    public void shouldBe_bb(){
        eventsToUndo.add(new TextInsertEvent(0,"aa"));
        eventsToUndo.add(new TextRemoveEvent(0,2));
        MyTextEvent eventToPerform = new TextInsertEvent(0,"bb");
        LinkedList<MyTextEvent> events = handler.undoTextEvents(eventsToUndo,eventToPerform);
        assertEquals(1, events.size());
        assertEquals(true, events.peek() instanceof TextInsertEvent);
        TextInsertEvent event = (TextInsertEvent) events.poll();
        assertEquals(0, event.getOffset());
        assertEquals("bb", event.getText());
    }

    @Test
    //Test text remove event followed by text insert event
    public void shouldBe_abxxf(){
        eventsToUndo.add(new TextRemoveEvent(1,3));
        eventsToUndo.add(new TextInsertEvent(0,"ab"));
        MyTextEvent eventToPerform = new TextInsertEvent(5,"f");
        LinkedList<MyTextEvent> events = handler.undoTextEvents(eventsToUndo,eventToPerform);
        assertEquals(1, events.size());
        assertEquals(true, events.peek() instanceof TextInsertEvent);
        TextInsertEvent event = (TextInsertEvent) events.poll();
        assertEquals(4, event.getOffset());
        assertEquals("f", event.getText());
    }

    @Test
    //Test two text remove event
    public void shouldBe_f(){
        eventsToUndo.add(new TextRemoveEvent(1,3));
        eventsToUndo.add(new TextRemoveEvent(0,2));
        MyTextEvent eventToPerform = new TextInsertEvent(4,"f");
        LinkedList<MyTextEvent> events = handler.undoTextEvents(eventsToUndo,eventToPerform);
        assertEquals(1, events.size());
        assertEquals(true, events.peek() instanceof TextInsertEvent);
        TextInsertEvent event = (TextInsertEvent) events.poll();
        assertEquals(0, event.getOffset());
        assertEquals("f", event.getText());
    }
}
