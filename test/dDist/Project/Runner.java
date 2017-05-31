package dDist.Project;

/**
 * Created by malthe on 30/05/2017.
 */
public class Runner {
    private static DistributedTextEditor dte1;
    private static DistributedTextEditor dte2;
    private static DistributedTextEditor dte3;
    private static DistributedTextEditor dte4;

    public static void main(String[] args) {
        dte1 = new DistributedTextEditor();
        dte2 = new DistributedTextEditor();
        dte3 = new DistributedTextEditor();
        dte4 = new DistributedTextEditor();
        dte1.setPortNumber("40499");
        dte1.listen();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        dte2.setPortNumber("40499");
        dte2.setIpaddress("127.0.0.1");
        dte2.connect();
        dte3.setPortNumber("40499");
        dte3.setIpaddress("127.0.0.1");
        dte4.setPortNumber("40499");
        dte4.setIpaddress("127.0.0.1");
    }
}
