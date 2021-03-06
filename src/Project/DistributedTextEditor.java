package Project;

import Project.packets.RequestConnectionsPacket;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.*;
import javax.swing.text.*;

public class DistributedTextEditor extends JFrame {
	private LinkedList<MyTextEvent> eventsPerformed;
	private LinkedBlockingQueue<Event> eventsToPerform;
	private JTextArea area1 = new JTextArea(50,120);
    private JTextField ipaddress = new JTextField("127.0.0.1");
    private JTextField portNumber = new JTextField("40499");

	public EventHandler eventHandler;
    
    private JFileChooser dialog = new JFileChooser(System.getProperty("user.dir"));

    private String currentFile = "Untitled";
    private boolean changed = false;
    private boolean listening = false;
	public DocumentEventCapturer dec;

    private int port = 40499;
    private ServerSocket serverSocket = null;
    private Socket socket;

	private String localAddress = "xxxx.xxxx.xxxx.xxxx";

	private HashMap<Integer, Integer> vectorClock;
	private Integer identifier;
	private boolean isConnected = false;
	private int originalPort = 0;
	private String originalIP = "";

	public DistributedTextEditor() {
    	eventsPerformed = new LinkedList<>();
		eventsToPerform = new LinkedBlockingQueue<>();
		dec = new DocumentEventCapturer(eventsToPerform,this);
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

		vectorClock = new HashMap<>();
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
		edit.getItem(1).setText("Paste");//Nothing much
		Save.setEnabled(false);
		SaveAs.setEnabled(false);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		area1.addKeyListener(k1);
		setTitle("Disconnected");
		setVisible(true);

		eventHandler = new EventHandler(eventsToPerform,this);
		eventHandler.start();
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
	    	listen();
	    }
	};

	public void listen() {
		saveOld();
		port = Integer.parseInt(portNumber.getText());
		setTitle("I'm listening on " + localAddress +":"+port);
		area1.setEditable(false);
		DistributedTextEditor dte  = this;
		listening = true;
		new Thread(new Runnable() {
			public void run() {
				try {
					serverSocket = new ServerSocket(port);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				try {
					socket = serverSocket.accept();
					Connection connection = new Connection(socket,eventsToPerform, eventHandler, dte);
					eventHandler.addConnection(connection);
					serverSocket.close();
					localAddress = socket.getLocalSocketAddress().toString();
					vectorClock.put(0, 0);
					identifier = 0;
					listening = false;
					isConnected = true;
				} catch (IOException e1) {
					if(e1 instanceof SocketException)
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
				startConnectedListener();
			}
		}).start();
	}

	public void startConnectedListener() {
		isConnected = true;
		DistributedTextEditor dte = this;
		new Thread(new Runnable() {
			public void run() {
				port = port + getIdentifier();
				setTitle(getTitle() + " | Listening for further connections on port: " + port);
				try {
					serverSocket = new ServerSocket(port);
				} catch (IOException e) {
					e.printStackTrace();
				}
				while(isConnected) {
					try {
						socket = serverSocket.accept();
						Connection connection = new Connection(socket, eventsToPerform, eventHandler, dte);
						eventHandler.addConnection(connection);
					} catch (IOException e) {
						if(e instanceof SocketException);
						else e.printStackTrace();
					}
				}
			}
		}).start();
	}

	Action Connect = new AbstractAction("Connect") {
	    public void actionPerformed(ActionEvent e) {
	    	connect();
	    }
	};

    public void connect(){
		saveOld();
		setTitle("Connecting to " + ipaddress.getText() + ":" + portNumber.getText() + "...");
		port = Integer.parseInt(portNumber.getText());
		try {
			originalIP = ipaddress.getText();
			originalPort = port;
			socket = new Socket(ipaddress.getText(), port);
			Connection connection = new Connection(socket,eventsToPerform, eventHandler, this);
			eventHandler.addConnection(connection);
			connection.send(new RequestConnectionsPacket());
			localAddress = socket.getLocalSocketAddress().toString();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		setTitle("Connected to " + socket.getRemoteSocketAddress());
		changed = false;
		Save.setEnabled(false);
		SaveAs.setEnabled(false);
	}

    Action Disconnect = new AbstractAction("Disconnect") {
	    public void actionPerformed(ActionEvent e) {
			if(!listening) {
				disconnect();
				if(isConnected){
					isConnected = false;
					try {
						serverSocket.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
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

	public void disconnect() {
		identifier = 0;
		vectorClock = new HashMap<>();
		setTitle("Disconnected");
		eventHandler.disconnect();
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

	public void setIpaddress(String ipaddress) {
		this.ipaddress.setText(ipaddress);
	}

	public void setPortNumber(String portNumber) {
		this.portNumber.setText(portNumber);
	}

	public String getLocalAddress() {
		return localAddress;
	}

	public HashMap<Integer, Integer> getVectorClock() {
		return vectorClock;
	}

	public void setVectorClock(HashMap<Integer, Integer> vectorClock) {
		this.vectorClock = vectorClock;
	}
    
    public static void main(String[] arg) {
    	new DistributedTextEditor();
    }

	public Integer getIdentifier() {
		return identifier;
	}

	public LinkedList<MyTextEvent> getEventsPerformed(){
    	return eventsPerformed;
	}

	public JTextArea getArea(){
    	return area1;
	}

	public int getOriginalPort() {
		return originalPort;
	}

	public String getOriginalIP() {
		return originalIP;
	}

	public void setIdentifier(int identifier) {
		this.identifier = identifier;
	}
}
