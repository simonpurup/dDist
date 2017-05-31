package dDist.Project;

import Project.DistributedTextEditor;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by malthe on 30/05/2017.
 */
public class MultiPeerTest {
    DistributedTextEditor dte1;
    DistributedTextEditor dte2;
    DistributedTextEditor dte3;
    DistributedTextEditor dte4;

    @Before
    public void before(){
        dte1 = new DistributedTextEditor();
        dte2 = new DistributedTextEditor();
        dte3 = new DistributedTextEditor();
        dte4 = new DistributedTextEditor();
        dte1.setPortNumber("40499");
        dte1.listen();
        try { Thread.sleep(200);} catch (InterruptedException e) {e.printStackTrace();}
        dte2.setPortNumber("40499");
        dte2.setIpaddress("127.0.0.1");
        dte2.connect();
        try { Thread.sleep(200);} catch (InterruptedException e) {e.printStackTrace();}
        new Thread(new Runnable() {
            public void run() {
                dte3.setPortNumber("40499");
                dte3.setIpaddress("127.0.0.1");
                dte3.connect();
            }
        }).start();
    }

    @Test
    public void testConnectionsList(){
    }
}
