package Protocol;
import java.io.IOException;

//Classes that define the behaviour of the Protocol Interpreter based on the current state
public class State {
	//Return a message to be printed for the user
	public String messageForUser(ProtocolInterpreter PI){
		return null;
	}

	//Must be overridden if there's need of user input
	public boolean needUserInput(ProtocolInterpreter PI){
		return false;
	}				

	//Used to get commands or data from the user
	public void userInputEval(String string,ProtocolInterpreter PI) throws Exception{
		throw new Exception("Getting the input in an undesired moment O.o\n");
	}

	//Write on the socket a message (non need to override)
	public void sendMessageToServer(ProtocolInterpreter PI) throws Exception{
		PI.ps.print(PI.toBeSent);
		PI.toBeSent = null;
	}

	//Method to receive a response from the server
	//		probably no need to override
	public String receiveMessage(ProtocolInterpreter PI) throws IOException{
		StringBuilder builder = new StringBuilder();
		String line;
		do {
			line = PI.bis.readLine();
			if (line == null) {
				break ;
			}
			if (line.length() > 0) {
				builder.append(line).append ("\n");
			}
		}while (line.length() > 0 && line.matches("[0-9]{3}-.*"));
		PI.lastResponse = builder.toString();
		return PI.lastResponse;
	}

	public Integer lastResponseCode(ProtocolInterpreter PI){
		String[] lines = PI.lastResponse.split("\n");
		//Returns the number represented by the first three character of
		//the last line of the last response; null if error occurred
		return Integer.parseInt(lines[lines.length-1].substring(0, 3));
	}

	//Method to parse the response of the server
	//		MUST be overridden
	public String interpretServerResponse(ProtocolInterpreter PI) throws Exception{
		throw new Exception("Cannot parse... I know nothing\n");
		//return null;
	}

	public static class Connected extends State{
		//If 220 response then go in AUTHSENDUSER state
		public String interpretServerResponse(ProtocolInterpreter PI) throws Exception{
			Integer responseCode;
			try{
				responseCode = lastResponseCode(PI);
			}catch(NumberFormatException e){
				PI.currentState = StateEnum.ERROR;	//Error
				return null;
			}
			StringBuilder forUser = new StringBuilder();

			forUser.append(PI.lastResponse).append('\n');

			if(responseCode.intValue() == 220){
				forUser.append("Connection success\n");
				PI.currentState = StateEnum.AUTHSENDUSER; //AuthSendUser
			}else{
				forUser.append("An error occured in the connection.\nResponse code: ").append(responseCode.toString());
				PI.currentState = StateEnum.ERROR;
			}
			return forUser.toString();
		}
	}

	public static class AuthSendUser extends State{
		public String messageForUser(ProtocolInterpreter PI){
			return "Username: ";
		}

		public boolean needUserInput(ProtocolInterpreter PI){
			return true;
		}
		//Prepare the message to be sent with the user data
		public void userInputEval(String string,ProtocolInterpreter PI) throws Exception{
			PI.toBeSent = "USER "+string+"\r\n";
		}
		//Check if need of the password then go to AUTHSENDPASSWD state
		//Else to the READY state
		public String interpretServerResponse(ProtocolInterpreter PI) throws Exception{
			Integer responseCode;
			try{
				responseCode = lastResponseCode(PI);
			}catch(NumberFormatException e){
				PI.currentState = StateEnum.ERROR;	//Error
				return null;
			}
			StringBuilder forUser = new StringBuilder();

			forUser.append(PI.lastResponse).append('\n');
			if(responseCode.intValue() == 331){
				PI.currentState = StateEnum.AUTHSENDPASSWD;
			}else if(responseCode.intValue() == 230){
				forUser.append("User has been authenticated\n");
				PI.currentState = StateEnum.READY;
			}else{
				forUser.append("An error occured in the connection.\nResponse code: ").append(responseCode.toString());
				PI.currentState = StateEnum.ERROR;
			}
			return forUser.toString();
		}
	}

	public static class AuthSendPasswd extends State{
		public String messageForUser(ProtocolInterpreter PI){
			return "Password: ";
		}

		public boolean needUserInput(ProtocolInterpreter PI){
			return true;
		}

		public void userInputEval(String string,ProtocolInterpreter PI) throws Exception{
			PI.toBeSent = "PASS "+string+"\r\n";
		}
		//If good answer from the server then go in READY state
		public String interpretServerResponse(ProtocolInterpreter PI) throws Exception{
			Integer responseCode;
			try{
				responseCode = lastResponseCode(PI);
			}catch(NumberFormatException e){
				PI.currentState = StateEnum.ERROR;	//Error
				return null;
			}
			StringBuilder forUser = new StringBuilder();

			forUser.append(PI.lastResponse).append('\n');
			if(responseCode.intValue() == 230){
				forUser.append("User has been authenticated\n");
				PI.currentState = StateEnum.READY;
			}else{
				forUser.append("An error occured in the connection.\nResponse code: ").append(responseCode.toString());
				PI.currentState = StateEnum.ERROR;
			}
			return forUser.toString();
		}
	}

	public static class Ready extends State{
		boolean after_sending_open_data_socket = false;
		//Lists the possible commands for the user
		public String messageForUser(ProtocolInterpreter PI){
			String commandList = new StringBuilder()
			.append("ls - list the content of current directory\n")
			.append("cd [directory] - changes the current directory to [directory]\n")
			.append("get [file] - download [file]\n")
			.append("put [file] - upload [file] \n")
			.append("exit - close the connection and exit\n")
			.toString();
			String prompt = "? - ";
			return commandList+prompt;
		}

		public boolean needUserInput(ProtocolInterpreter PI){
			return true;
		}
		//Reads the command of the user and execute the corresponding functions
		public void userInputEval(String fromUser, ProtocolInterpreter PI){
			PI.currentUserCommand = fromUser.trim();
			try{
				if(PI.currentUserCommand.startsWith("ls")){
					PI.commandPORT();
				}else if(PI.currentUserCommand.startsWith("cd")){
					String arg = null;
					try{
						arg = fromUser.split("[ \t]+")[1];
					}catch(ArrayIndexOutOfBoundsException e){
						arg = ".";
					}
					PI.toBeSent = "CWD "+arg+"\r\n";
				}else if(PI.currentUserCommand.startsWith("get")){
					PI.commandPORT();
				}else if(PI.currentUserCommand.startsWith("put")){
					PI.commandPORT();
				}else if(PI.currentUserCommand.startsWith("exit")){
					PI.commandQUIT();
					PI.currentState = StateEnum.EXITING;
				}
			}catch(IOException e){
				PI.currentState = StateEnum.ERROR;
			}
		}

		public String interpretServerResponse(ProtocolInterpreter PI) throws Exception{
			Integer responseCode;
			String forUser = null;
			try{
				responseCode = lastResponseCode(PI);
				if(PI.currentUserCommand.startsWith("ls") || PI.currentUserCommand.startsWith("get")){
					if(responseCode == 200){
						forUser = "PORT command succeded\n";
						PI.openDataConnection();
						PI.currentState = StateEnum.EXECUTING;
					}
				}
			}catch(NumberFormatException e){
				PI.currentState = StateEnum.ERROR;	//Error
				return forUser;
			}
			return forUser;
		}
	}

	public static class Executing extends State{
		private String fileName = null;

		public String messageForUser(ProtocolInterpreter PI){
			String info = new StringBuilder()
			.append("Preparing for download\n")
			.toString();
			return info;
		}

		public void sendMessageToServer(ProtocolInterpreter PI) throws Exception {
			try{
				if(PI.currentUserCommand.startsWith("ls")){
					PI.toBeSent = "LIST\r\n";
				}else if(PI.currentUserCommand.startsWith("get")){
					String[] arg = PI.currentUserCommand.split("[ \t]+");
					this.fileName = arg[1];
					PI.toBeSent = "RETR "+this.fileName+"\r\n";
				}
				super.sendMessageToServer(PI);
			}catch(IOException e){
				PI.currentState = StateEnum.ERROR;
			}
		}

		public String interpretServerResponse(ProtocolInterpreter PI){
			Integer responseCode;
			StringBuilder forUser = new StringBuilder();
			try{
				responseCode = lastResponseCode(PI);
				
				if(responseCode == 150 || responseCode == 125){
					forUser.append("Starting to download...\n");
					if(PI.currentUserCommand.equals("ls")){
						PI.prepareFile("@stdout");
					}else if(PI.currentUserCommand.equals("get")){
						System.err.print("Writing "+ this.fileName +" in filename\n");
						PI.prepareFile(this.fileName);
					}
					PI.currentState = StateEnum.DOWNLOADING;
				}else{
					 forUser.append("Maybe the file does not exist, or you don't have the rights to download it\n");
				}	
			}catch(NumberFormatException e){
				PI.currentState = StateEnum.ERROR;	//Error
			}
			return forUser.toString();
		}
	}
	
	public static class Downloading extends State{

		public String messageForUser(ProtocolInterpreter PI){
			try{
				if(PI.filename.equals("@stdout")){
					PI.writeDataOnStdout();
				}else{
					PI.writeDataOnFile(PI.filename);
				}
			}catch(IOException e){
				e.printStackTrace();
				PI.currentState = StateEnum.ERROR;
			}
			String info = "\n";
			return info;
		}

		public void sendMessageToServer(ProtocolInterpreter PI) throws Exception {
			;
		}

		public String interpretServerResponse(ProtocolInterpreter PI){
			Integer responseCode;
			StringBuilder forUser = new StringBuilder();
			try{
				responseCode = lastResponseCode(PI);
				if(responseCode == 226 || responseCode == 250){
					forUser.append(PI.lastResponse)
						.append("\nCommand successfully completed\n");
					PI.currentState = StateEnum.READY;
				}else{
					PI.currentState = StateEnum.ERROR;
				}
			}catch(NumberFormatException e){
				PI.currentState = StateEnum.ERROR;	//Error
			}
			PI.currentUserCommand = null;
			return forUser.toString();
		}
	}
	
	public static class Exiting extends State{
		public String interpretServerResponse(ProtocolInterpreter PI) throws Exception{
			String forUser = null;
			try{
				forUser = "Exiting...";
				PI.bis.close();
				PI.data_socket.close();
				PI.ps.close();
				PI.server_data_socket.close();
				PI.socket.close();
			}catch(NumberFormatException e){
				PI.currentState = StateEnum.ERROR;	//Error
				return forUser;
			}
			PI.alive = false;
			return forUser;
		}
	}

	public static class Error extends State{
		//Return a message to be printed for the user
		public String messageForUser(ProtocolInterpreter PI){
			return "A fatal error occured\n";
		}

		//Must be overridden if there's need of user input
		public boolean needUserInput(ProtocolInterpreter PI){
			return false;
		}

		//Write on the socket a message (non need to override)
		public void sendMessageToServer(ProtocolInterpreter PI) throws Exception{
			PI.toBeSent = null;
		}

		//Method to receive a response from the server
		//		probably no need to override
		public String receiveMessage(ProtocolInterpreter PI) throws IOException{
			return null;
		}

		public Integer lastResponseCode(ProtocolInterpreter PI){
			return -1;
		}

		//Method to parse the response of the server
		//		MUST be overridden
		public String interpretServerResponse(ProtocolInterpreter PI){
			PI.alive = false;
			return null;
		}
	}
}