package Project;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by malthe on 29/05/2017.
 */
public class TwoPeerTest {

    DistributedTextEditor dte1;
    DistributedTextEditor dte2;

    @BeforeEach
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

    @AfterEach
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
