package Project;
/**
 * Created by lærerPC on 30-04-2017.
 */

import Project.DistributedTextEditor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.Assert.assertEquals;

public class MyTest1 {

    DistributedTextEditor dte1;
    DistributedTextEditor dte2;

    @BeforeEach
    public void init(){
        dte1 = new DistributedTextEditor();
        dte2 = new DistributedTextEditor();
        dte1.setPortNumber("40499");
        dte1.listen();
        try {Thread.sleep(200);} catch (InterruptedException e) {}
        dte2.setPortNumber("40499");
        dte2.setIpaddress("127.0.1.1");
        dte2.connect();
        try {Thread.sleep(100);} catch (InterruptedException e) {}
    }

    @Test
    public void test1(){
        assertEquals(Math.toIntExact(dte1.getVectorClock().get(dte1.getLocalAddress())),0);
        addTextInsert("a",0,dte1.getArea1());
        try {Thread.sleep(100);} catch (InterruptedException e) {}
        assertEquals(Math.toIntExact(dte1.getVectorClock().get(dte1.getLocalAddress())),1);
        assertEquals(Math.toIntExact(dte2.getVectorClock().get(dte1.getLocalAddress())),1);
        assertEquals(Math.toIntExact(dte2.getVectorClock().get(dte2.getLocalAddress())),0);
        assertEquals(dte2.getArea1().getText(), "a" );
    }

    @Test
    public void test2(){
        addTextInsert("a",0,dte1.getArea1());
        addTextInsert("b",0,dte2.getArea1());
        try {Thread.sleep(1000);} catch (InterruptedException e) {}
        assertEquals(dte2.getArea1().getText(), "ab" );
        assertEquals(dte1.getArea1().getText(), "ab" );
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
