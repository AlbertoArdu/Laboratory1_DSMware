package FTPClient;
import java.io.*;
import java.net.*;

import Protocol.*;

public class Client {

	public static void main(String[] args) {
		try {
			if (args.length < 2) {
				System.out.println("Usage: java ip port");
				return;
			}
			
			InetAddress remote = InetAddress.getByName(args[0]);
			ProtocolInterpreter PI = new ProtocolInterpreter(remote,Integer.parseInt(args[1]));
			BufferedReader userInputStream = new BufferedReader(new InputStreamReader(System.in));
			String recv;
			String string, fromUser;
			
			PI.connect();
			recv = PI.receiveMessage();
			System.err.println(recv);
			string = PI.interpretServerResponse();
			if(string != null)
				System.out.print(string);
			
			while(PI.alive){
				string = PI.messageForUser();
				if(string != null)
					System.out.print(string);
				if(PI.needUserInput()){
					fromUser = userInputStream.readLine();
					while(userInputStream.ready()){
						userInputStream.read();
					}
					PI.userInputEval(fromUser);
				}
				PI.sendMessageToServer();
				recv = PI.receiveMessage();
				System.err.println(recv);
				//Here change the state
				string = PI.interpretServerResponse();
				if(string != null)
					System.out.print(string);
			}
			
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
		}
	}
	
	static int getCommand(){
		System.out.println();
		return 0;
	}
}
