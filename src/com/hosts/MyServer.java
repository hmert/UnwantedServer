package com.hosts;

/* 
 * UnwantedSite Server
 * Simple TCP HTTP Server that can edit hosts file
 * Run it as administrator
 * Then hit http://serverIP:5000/willAddToHosts.com
 * It appends as "127.0.0.1 willAddToHosts.com"
 */


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.StringTokenizer;


public class MyServer extends Thread {

	static final String HTML_START = "<html>"
			+ "<title>HTTP Server in java</title>" + "<body>";

	static final String HTML_END = "</body>" + "</html>";

	Socket connectedClient = null;
	BufferedReader inFromClient = null;
	DataOutputStream outToClient = null;

	public MyServer(Socket client) {
		connectedClient = client;
	}

	@SuppressWarnings("deprecation")
	public void run() {

		try {

			inFromClient = new BufferedReader(new InputStreamReader(
					connectedClient.getInputStream()));
			outToClient = new DataOutputStream(
					connectedClient.getOutputStream());

			String requestString = inFromClient.readLine();
			String headerLine = requestString;
			
			StringTokenizer tokenizer = new StringTokenizer(headerLine);
			String httpMethod = tokenizer.nextToken();
			String httpQueryString = tokenizer.nextToken();

			StringBuffer responseBuffer = new StringBuffer();
			responseBuffer.append("Ev<BR>");
			responseBuffer.append("request:<BR>");

			while (inFromClient.ready()) {
				// Query String okutmaya calistim
				responseBuffer.append(requestString + "<BR>");
				//System.out.println(requestString);
				requestString = inFromClient.readLine();
			}

			if (httpMethod.equals("GET")) {
				if (httpQueryString.equals("/")) {
					sendResponse(200, responseBuffer.toString(), false);
				} else {
					String fileName = httpQueryString.replaceFirst("/", "");
					fileName = URLDecoder.decode(fileName);

					if (new File(fileName).isFile()) {
						sendResponse(200, fileName, true);
					} else {

						// ignore these
						
						if(fileName!="favicon.ico" || fileName!="robots.txt"){

							try {
								FileWriter fstream = new FileWriter(
										"c:\\Windows\\system32\\drivers\\etc\\hosts", // for Windows
										//	"/etc/hosts",							  // for Unix
										true);
								
								BufferedWriter out = new BufferedWriter(fstream);
								out.write("\n");
								out.write("127.0.0.1 " + fileName);
								out.close();
							} catch (Exception e) {
								System.out.println("Error: " + e.getMessage());
							}
						}

						sendResponse(404, "404", false);
					}
				}
			} else
				sendResponse(404, "404", false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendResponse(int statusCode, String responseString,
			boolean isFile) throws Exception {

		String statusLine = null;
		String serverdetails = "Server: Http";
		String contentLengthLine = null;
		String fileName = null;
		String contentTypeLine = "Content-Type: text/html" + "\r\n";
		FileInputStream fin = null;

		if (statusCode == 200)
			statusLine = "HTTP/1.1 200 OK" + "\r\n";
		else
			statusLine = "HTTP/1.1 404 Not Found" + "\r\n";

		if (isFile) {
			fileName = responseString;
			fin = new FileInputStream(fileName);
			contentLengthLine = "Content-Length: "
					+ Integer.toString(fin.available()) + "\r\n";
			if (!fileName.endsWith(".htm") && !fileName.endsWith(".html"))
				contentTypeLine = "Content-Type: \r\n";
		} else {
			responseString = MyServer.HTML_START + responseString
					+ MyServer.HTML_END;
			contentLengthLine = "Content-Length: " + responseString.length()
					+ "\r\n";
		}

		outToClient.writeBytes(statusLine);
		outToClient.writeBytes(serverdetails);
		outToClient.writeBytes(contentTypeLine);
		outToClient.writeBytes(contentLengthLine);
		outToClient.writeBytes("Connection: close\r\n");
		outToClient.writeBytes("\r\n");

		if (isFile)
			sendFile(fin, outToClient);
		else
			outToClient.writeBytes(responseString);

		outToClient.close();
	}

	public void sendFile(FileInputStream fin, DataOutputStream out)
			throws Exception {
		byte[] buffer = new byte[1024];
		int bytesRead;

		while ((bytesRead = fin.read(buffer)) != -1) {
			out.write(buffer, 0, bytesRead);
		}
		fin.close();
	}

	static public String getContents(File aFile) {

		StringBuilder contents = new StringBuilder();

		try {

			BufferedReader input = new BufferedReader(new FileReader(aFile));
			try {
				String line = null;

				while ((line = input.readLine()) != null) {
					contents.append(line);
					contents.append(System.getProperty("line.separator"));
				}
			} finally {
				input.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return contents.toString();
	}


	public static void main(String args[]) throws Exception {
		ServerSocket Server = new ServerSocket(5000, 10,
				InetAddress.getByName("0.0.0.0"));
		System.out.println("Working at port 5000");
		while (true) {
			Socket connected = Server.accept();
			(new MyServer(connected)).start();
		}
	}
}
