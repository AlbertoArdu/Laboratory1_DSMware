package Protocol;

import java.io.*;
import java.net.*;

enum StateEnum {
	CONNECTED,
	AUTHSENDUSER,
	AUTHSENDPASSWD,
	READY,
	EXECUTING,
	DOWNLOADING,
	EXITING,
	ERROR;
}

public class ProtocolInterpreter {
	
	protected InetAddress server;
	protected int port;
	protected Socket socket;
	protected InetAddress local;
	protected int data_port;
	protected ServerSocket server_data_socket = null;
	protected Socket data_socket = null;
	protected BufferedReader bis;
	protected PrintStream ps;
	protected String lastResponse;
	protected String toBeSent;
	protected State[] states = {
			new State.Connected(),
			new State.AuthSendUser(),
			new State.AuthSendPasswd(),
			new State.Ready(),
			new State.Executing(),
			new State.Downloading(),
			new State.Exiting(),
			new State.Error()
	};
	protected StateEnum currentState;
	protected String currentUserCommand;
	public boolean alive;
	
	public String filename = null;
	public String path = "C:\\Users\\ardus\\workspace\\tmp\\";

	public ProtocolInterpreter(InetAddress server,int port){
		this.server = server;
		this.port = port;
	}

	public void connect() throws IOException{
		this.socket = new Socket(server,port);
		local = socket.getLocalAddress();
		bis = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		ps = new PrintStream(socket.getOutputStream());
		currentState = StateEnum.CONNECTED;
		alive = true;
	}

	public String receiveMessage() throws IOException{
		return states[currentState.ordinal()].receiveMessage(this);
	}

	//Most likely changes the current state
	public String interpretServerResponse() throws Exception{
		String toShowToUser = states[currentState.ordinal()].interpretServerResponse(this);
		lastResponse = null;
		return toShowToUser;
	}

	public String messageForUser(){
		return states[currentState.ordinal()].messageForUser(this);
	}

	public boolean needUserInput(){
		return states[currentState.ordinal()].needUserInput(this);
	}

	public void userInputEval(String string)throws Exception{
		states[currentState.ordinal()].userInputEval(string,this);
	}

	public void sendMessageToServer()throws Exception{
		states[currentState.ordinal()].sendMessageToServer(this);
		toBeSent = null;
	}

	public Integer lastResponeCode(){
		return states[currentState.ordinal()].lastResponseCode(this);
	}

	//If there is no socket listening, search for free port
	public void createDataSocket() throws IOException{
		if(server_data_socket != null){
			try{
				server_data_socket.close();
			}catch(Exception e){
				System.err.println("Strange but we continue");
			}
			server_data_socket = null;
		}
		server_data_socket = new ServerSocket();
		InetSocketAddress addr = new InetSocketAddress(local, 0);
		server_data_socket.bind(addr);

		data_port = server_data_socket.getLocalPort();
		
	}

	public void openDataConnection() throws IOException{
		data_socket = server_data_socket.accept();
	}
	
	public void prepareFile(String filename){
		this.filename = filename;
	}
	
	public void writeDataOnStdout() throws IOException{
		InputStreamReader input;
		char[] buffer = new char[8*1024];
		int n;

		input = new InputStreamReader(data_socket.getInputStream());
		do {
			n = input.read(buffer);
			if (n == 0 || n == -1)
				break ;
			System.out.print(String.valueOf(buffer, 0, n));
		}while (true);
		System.err.println("File successfully downloaded");
	}
	
	public void writeDataOnFile(String filename) throws IOException{
		InputStreamReader input;
		char[] buffer = new char[8*1024];
		int n;
		BufferedWriter downloadedFile;
		
		downloadedFile = new BufferedWriter(new FileWriter(path + filename));
		
		input = new InputStreamReader(data_socket.getInputStream());
		if(downloadedFile != null){
			try{
				do {
					n = input.read(buffer);
					if (n == 0 || n == -1)
						break ;
					downloadedFile.write(buffer, 0, n);
				}while (true);
			}finally{
				downloadedFile.close();
				downloadedFile = null;
			}
		}
		System.err.println("File successfully downloaded");
	}

	//Ftp commands
	public void commandPORT() throws IOException{
		int port_h,port_l;
		String[] ip;

		this.createDataSocket();
		ip = local.getHostAddress().split("\\.");
		port_h = data_port/256;
		port_l = data_port%256;

		toBeSent = new StringBuilder("PORT ")
						.append(ip[0])
						.append(",")
						.append(ip[1])
						.append(",")
						.append(ip[2])
						.append(",")
						.append(ip[3])
						.append(",")
						.append(port_h)
						.append(",")
						.append(port_l)
						.append("\r\n")
						.toString();
		System.err.println(toBeSent);
	}

	public void commandQUIT(){
		toBeSent = "QUIT\r\n";
	}
}
