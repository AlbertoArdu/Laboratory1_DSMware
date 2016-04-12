//HttpServer.java

import java.net.*;
import java.util.*;
import java.io.*;


class HttpServer {
	PrintStream ps;

	int state = 0;
	HashMap<InetAddress,Integer> clients;
	int i, j;

	public void initWebServer(int port) {
		try {
			// byte[] buf = new byte[1000];
			int i,j;
			// InputStream is;
			BufferedReader bis;
			String page_requested = "/"; // page requested ("/" by default)
			ServerSocket master_sock = null;
			InetAddress currentAddr;
			try {
				master_sock = new ServerSocket(port);
			} catch (Exception e) {
				System.err.println(e);
				e.printStackTrace();
			}
			Socket socket;
			System.err.println("Web Server started");
			String requestLine;
			Integer currentState;
			//Instantiate clients
			clients = new HashMap<InetAddress,Integer>(20);

			while (true) {
				page_requested = "/";
				socket = master_sock.accept();
				
				currentAddr = socket.getInetAddress();
				currentState = clients.get(currentAddr);
				if(currentState == null)
					currentState = new Integer(0);
				System.err
						.println("-----------------------------------------------");

				bis = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				ps = new PrintStream(socket.getOutputStream());

				// display request contents
				System.err.println();
				System.err.println("REQUEST:");
				System.err.println("--------");
				try {
					// Request
					requestLine = bis.readLine();
					System.err.println(requestLine);
					// Parse request
					if(requestLine==null){
						continue;
					}
					i = requestLine.indexOf("GET");
					j = requestLine.indexOf("HTTP");
					if (i == -1) {
						System.err.println("Invalid request type!"
								+ requestLine);
						System.exit(-1);
					}
					i += 3;
					String page = requestLine.substring(i,j);
					page = page.trim();
					if (page.equals(page_requested)) {
						page = "/page1";
					}
					page_requested = page;
					state=0;
					while ((requestLine = bis.readLine()) != null) {
						// Headers (Ignored);
						System.err.println(requestLine);
						if(requestLine.length() <= 2)
							break;
						if(requestLine.startsWith("Cookie: state=")){
							state = Integer.parseInt(requestLine.substring(14));
							System.err.println(state);
						}
					}	
				} catch (IOException e) {
					;// Possible that the request is finished
				}
				System.err.println();
				System.err.println("writing back page:" + page_requested);
				displayPage(page_requested,currentAddr,currentState);
				socket.close();
				
			}
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
		}
	}

	public void sendHeader(int state) {
		System.err.println("Sending header");
		ps.print("HTTP/1.1 200 OK\r\n");
		ps.print("Connection:\t close\r\n");
		ps.print("Content-Type: text/html\r\n");
		ps.print("Set-Cookie: state=" + state + "\r\n");
		ps.print("\r\n");
		System.err.println("Header sent");
	}

	public void sendError() {
		ps.print("HTTP/1.1 404 NOT FOUND\r\n");
		ps.print("Connection:\t close\r\n");
		ps.print("Content-Type: text/html\r\n");
		ps.print("\r\n\r\n");
		ps.println("<HTML>\n<title>404 - Not Found</title>");
		ps.println("<H1>Ahhhhhh</H1>");
		ps.println("<P>There's not such a page :P");
		ps.println("</P>");
		ps.println("");
		ps.println("<HR>\n</HTML>");
	}

	public void displayPage(String name, InetAddress current, Integer state) {
		if(name.equals("/page1")){
			sendHeader(1);
			clients.put(current,1);
			displayPage1();
		}else if(name.equals("/page2")){
			if(state == 1){
				sendHeader(2);
				clients.put(current,2);
				displayPage2();
			}else{
				displayPage2NotAuthorized();
			}
		}else if(name.equals("/page3")){
			if(state == 2){
				sendHeader(0);
				clients.put(current,0);
				displayPage3();
			}else{
				displayPage3NotAuthorized();
			}
		}else{
			sendError();
		}
		
	}
	public void displayPage1() {
		state = 1;
		System.err.println("Sending page");
		ps.println("<HTML>\n<title>Java Socket Web Server Page 1</title>");
		ps.println("<H1>Java Socket Web Server - Welcome to page 1</H1>");
		ps.println("<P>This server is powered by Java Sockets.");
		ps.println("This is not so neat, but not so big either</P>");
		ps.println("Want another cup of java? Click <A HREF=\"page2\">here</A>");
		ps.println("<HR>\n</HTML>");
		System.err.println("Page sent");
	}
	public void displayPage2() {
		state = 2;
		ps.println("<HTML>\n<title>Java Socket Web Server Page 2</title>");
		ps.println("<H1>Java Socket Web Server - Welcome to page 2</H1>");
		ps.println("<P>This server is powered by Java Sockets.");
		ps.println("This page is nearly the same as page 1, why bother?</P>");
		ps.println("Go to <A HREF=\"page3\">page 3</A>");
		ps.println("<HR>\n</HTML>");
	}
	public void displayPage3() {
		state = 0;
		ps.println("<HTML>\n<title>Java Socket Web Server Page 3</title>");
		ps.println("<H1>Java Socket Web Server - Welcome to page 3</H1>");
		ps.println("Page 3 at last !!!</P>");
		ps.println("<HR>\n</HTML>");
	}
	public void displayPage2NotAuthorized() {
		ps.println("<HTML>\n<title>Error !!</title>");
		ps.println("<H1>Page 2 cannot be accessed directly</H1>");
		ps.println("<H1>You must read page 1 !</H1>");
	}
	public void displayPage3NotAuthorized() {
		ps.println("<HTML>\n<title>Error !!</title>");
		ps.println("<H1>Page 3 cannot be accessed directly</H1>");
		ps.println("<H1>You must read page 1 and 2 first !</H1>");
	}
}
