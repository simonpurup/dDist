package Project;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.text.*;


public class DistributedTextEditor extends JFrame {

    private JTextArea area1 = new JTextArea(20,120);
    private JTextField ipaddress = new JTextField("IP address here");     
    private JTextField portNumber = new JTextField("Port number here");     
    
    private EventReplayer er;
    private Thread ert; 
    
    private JFileChooser dialog = 
    		new JFileChooser(System.getProperty("user.dir"));

    private String currentFile = "Untitled";
    private boolean changed = false;
    private boolean listening = false;
    private DocumentEventCapturer dec = new DocumentEventCapturer();

    private int port = 80;
    private ServerSocket serverSocket = null;
    private Socket socket;

	public String getLocalAddress() {
		return localAddress;
	}

	private String localAddress = "xxxx.xxxx.xxxx.xxxx";
	private final DistributedTextEditor dte = this;

	public HashMap<String, Integer> getVectorClock() {
		return vectorClock;
	}

	public void setVectorClock(HashMap<String, Integer> vectorClock) {
		this.vectorClock = vectorClock;
	}

	private HashMap<String, Integer> vectorClock;
    
    public DistributedTextEditor() {
		try {
			InetAddress localhost = InetAddress.getLocalHost();
			localAddress = localhost.getHostAddress();
		} catch (java.net.UnknownHostException e) {
			System.err.println("Cannot resolve the Internet address of the local host.");
			System.err.println(e);
			System.exit(-1);
			e.printStackTrace();
			e.printStackTrace();
		}

		vectorClock = new HashMap<String, Integer>();
		vectorClock.put(localAddress, 0);

		//Premade initialisation
    	area1.setFont(new Font("Monospaced",Font.PLAIN,12));
    	((AbstractDocument)area1.getDocument()).setDocumentFilter(dec);
    	
    	Container content = getContentPane();
    	content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
    	
    	JScrollPane scroll1 = 
    			new JScrollPane(area1, 
    					JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
    					JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    	content.add(scroll1,BorderLayout.CENTER);
		
		content.add(ipaddress,BorderLayout.CENTER);	
		content.add(portNumber,BorderLayout.CENTER);	
		
	JMenuBar JMB = new JMenuBar();
	setJMenuBar(JMB);
	JMenu file = new JMenu("File");
	JMenu edit = new JMenu("Edit");
	JMB.add(file); 
	JMB.add(edit);
	
	file.add(Listen);
	file.add(Connect);
	file.add(Disconnect);
	file.addSeparator();
	file.add(Save);
	file.add(SaveAs);
	file.add(Quit);
		
	edit.add(Copy);
	edit.add(Paste);
	edit.getItem(0).setText("Copy");
	edit.getItem(1).setText("Paste");

	Save.setEnabled(false);
	SaveAs.setEnabled(false);
		
	setDefaultCloseOperation(EXIT_ON_CLOSE);
	pack();
	area1.addKeyListener(k1);
	setTitle("Disconnected");
	setVisible(true);
	area1.insert("Example of how to capture stuff from the event queue and replay it in another buffer.\n" +
		     "Try to type and delete stuff in the top area.\n" + 
		     "Then figure out how it works.\n", 0);

	er = new EventReplayer(dec, area1, this);
	ert = new Thread(er);
	ert.start();
    }

    private KeyListener k1 = new KeyAdapter() {
	    public void keyPressed(KeyEvent e) {
		changed = true;
		Save.setEnabled(true);
		SaveAs.setEnabled(true);
	    }
	};

    Action Listen = new AbstractAction("Listen") {
	    public void actionPerformed(ActionEvent e) {
	    	saveOld();
	    	area1.setText("");
			port = Integer.parseInt(portNumber.getText());
			setTitle("I'm listening on " + localAddress +":"+port);
			area1.setEditable(false);
			listening = true;
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						serverSocket = new ServerSocket(port);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					try {
						socket = serverSocket.accept();
						serverSocket.close();
						listening = false;
					} catch (IOException e1) {
						if(e1 instanceof SocketException && e1.getMessage().equals("Socket closed"))
							if(listening)
							e1.printStackTrace();
					}
					if(socket != null) {
						setTitle("Connected to " + socket.getRemoteSocketAddress());
					} else{
						setTitle("Disconnected");
					}
					area1.setEditable(true);
					changed = false;
					Save.setEnabled(false);
					SaveAs.setEnabled(false);
				}
			}).start();
	    }
	};

    Action Connect = new AbstractAction("Connect") {
	    public void actionPerformed(ActionEvent e) {
	    	saveOld();
	    	area1.setText("");
	    	setTitle("Connecting to " + ipaddress.getText() + ":" + portNumber.getText() + "...");
            port = Integer.parseInt(portNumber.getText());
            try {
                socket = new Socket(ipaddress.getText(), port);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            setTitle("Connected to " + socket.getRemoteSocketAddress());
            changed = false;
	    	Save.setEnabled(false);
	    	SaveAs.setEnabled(false);
	    }
	};

    Action Disconnect = new AbstractAction("Disconnect") {
	    public void actionPerformed(ActionEvent e) {
	    	if(!listening)
				disconnect();
	    	else {
	    		listening = false;
				try {
					serverSocket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	};

	public void disconnect(){
		setTitle("Disconnected");
		area1.setText("");
		changed = false;
		Save.setEnabled(false);
		SaveAs.setEnabled(false);
	}

    Action Save = new AbstractAction("Save") {
	    public void actionPerformed(ActionEvent e) {
		if(!currentFile.equals("Untitled"))
		    saveFile(currentFile);
		else
		    saveFileAs();
	    }
	};

    Action SaveAs = new AbstractAction("Save as...") {
	    public void actionPerformed(ActionEvent e) {
	    	saveFileAs();
	    }
	};

    Action Quit = new AbstractAction("Quit") {
	    public void actionPerformed(ActionEvent e) {
	    	saveOld();
	    	System.exit(0);
	    }
	};
	
    ActionMap m = area1.getActionMap();

    Action Copy = m.get(DefaultEditorKit.copyAction);
    Action Paste = m.get(DefaultEditorKit.pasteAction);

    private void saveFileAs() {
	if(dialog.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
	    saveFile(dialog.getSelectedFile().getAbsolutePath());
    }
    
    private void saveOld() {
    	if(changed) {
	    if(JOptionPane.showConfirmDialog(this, "Would you like to save "+ currentFile +" ?","Save",JOptionPane.YES_NO_OPTION)== JOptionPane.YES_OPTION)
		saveFile(currentFile);
    	}
    }
    
    private void saveFile(String fileName) {
	try {
	    FileWriter w = new FileWriter(fileName);
	    area1.write(w);
	    w.close();
	    currentFile = fileName;
	    changed = false;
	    Save.setEnabled(false);
	}
	catch(IOException e) {
		e.printStackTrace();	}
    }

	public String getLocalAddress() {
		return localAddress;
	}

	public Socket getSocket() {
		return socket;
	}


    
    public static void main(String[] arg) {
    	new DistributedTextEditor();
    }


}
