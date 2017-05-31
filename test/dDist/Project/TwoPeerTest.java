package dDist.Project;

import Project.DistributedTextEditor;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import javax.swing.*;
import java.awt.*;

/**
 * Created by malthe on 29/05/2017.
 */
public class TwoPeerTest {

    DistributedTextEditor dte1;
    DistributedTextEditor dte2;

    @Before
    public void before(){
        dte1 = new DistributedTextEditor();
        dte2 = new DistributedTextEditor();
        try {
            dte1.setPortNumber("40499");
            dte1.listen();
            Thread.sleep(200);
            dte2.setPortNumber("40499");
            dte2.setIpaddress("127.0.0.1");
            dte2.connect();
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @After
    public void after(){
        dte1.disconnect();
        dte2.disconnect();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void vectorClockTest1(){
        assertEquals(0, (int)dte1.getVectorClock().get(dte1.getIdentifier()));
        assertEquals(0, (int)dte2.getVectorClock().get(dte2.getIdentifier()));
        addTextInsert("a",0,dte1.getArea());
        try {
            Thread.sleep(200);
            assertEquals(1, (int)dte1.getVectorClock().get(dte1.getIdentifier()));
            assertEquals(0, (int)dte1.getVectorClock().get(dte2.getIdentifier()));
            assertEquals(1, (int)dte2.getVectorClock().get(dte1.getIdentifier()));
            assertEquals(0, (int)dte2.getVectorClock().get(dte2.getIdentifier()));
            addTextInsert("a",0,dte2.getArea());
            addTextInsert("b",0,dte2.getArea());
            Thread.sleep(200);
            assertEquals(1, (int)dte2.getVectorClock().get(dte1.getIdentifier()));
            assertEquals(2, (int)dte2.getVectorClock().get(dte2.getIdentifier()));
            assertEquals(1, (int)dte1.getVectorClock().get(dte1.getIdentifier()));
            assertEquals(2, (int)dte1.getVectorClock().get(dte2.getIdentifier()));

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void vectorClockTest2(){
        addTextInsert("a",0,dte2.getArea());
        addTextInsert("b",0,dte2.getArea());
        try {
            Thread.sleep(200);
            assertEquals(0, (int)dte2.getVectorClock().get(dte1.getIdentifier()));
            assertEquals(2, (int)dte2.getVectorClock().get(dte2.getIdentifier()));
            assertEquals(0, (int)dte1.getVectorClock().get(dte1.getIdentifier()));
            assertEquals(2, (int)dte1.getVectorClock().get(dte2.getIdentifier()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void textTest1(){
        addTextInsert("a",0,dte1.getArea());
        addTextInsert("b",0,dte1.getArea());
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals("ba",dte1.getArea().getText());
        assertEquals("ba",dte2.getArea().getText());
        addTextInsert("c",0,dte1.getArea());
        addTextInsert("d",0,dte1.getArea());
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals("dcba",dte1.getArea().getText());
        assertEquals("dcba",dte2.getArea().getText());
    }

    @Test
    public void causalTest1(){
        addTextInsert("a",0,dte1.getArea());
        addTextInsert("b",0,dte2.getArea());
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(dte1.getArea().getText(),dte2.getArea().getText());
    }

    @Test
    public void causalTest2(){
        addTextInsert("a",0,dte1.getArea());
        addTextInsert("b",0,dte2.getArea());
        addTextInsert("a",0,dte1.getArea());
        addTextInsert("b",0,dte2.getArea());
        try {Thread.sleep(200);} catch (InterruptedException e) {}
        assertEquals(dte1.getArea().getText(), dte2.getArea().getText());
    }

    @Test
    public void shouldBeText_Testb_texst(){
        addTextInsert("Test text",0,dte1.getArea());
        try {Thread.sleep(200);} catch (InterruptedException e) {}
        addTextInsert("b",4,dte2.getArea());
        addTextInsert("s",9,dte1.getArea());
        try {Thread.sleep(200);} catch (InterruptedException e) {}
        assertEquals(dte1.getArea().getText(), dte2.getArea().getText());
    }

    @Test
    public void removalTest(){
        addTextInsert("Aa",0,dte1.getArea());
        addTextRemove(0,1,dte1.getArea());
        try {Thread.sleep(200);} catch (InterruptedException e) {}
        assertEquals("a",dte1.getArea().getText());
        assertEquals("a",dte2.getArea().getText());
    }

    @Test
    public void causalRemovalTest(){
        addTextInsert("cc",0,dte2.getArea());
        addTextInsert("Aa",0,dte1.getArea());
        addTextRemove(0,2,dte2.getArea());
        try {Thread.sleep(200);} catch (InterruptedException e) {}
        assertEquals(dte2.getArea().getText(), dte1.getArea().getText());
    }

    public void addTextInsert(String text, int offset, JTextArea area){
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    area.insert(text, offset);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void addTextRemove(int offset, int length, JTextArea area){
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    area.replaceRange(null, offset, offset + length);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
