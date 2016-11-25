/*
 *  Java OTR library
 *  Copyright (C) 2008-2009  Ian Goldberg, Muhaimeen Ashraf, Andrew Chung,
 *                           Can Tang
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of version 2.1 of the GNU Lesser General
 *  Public License as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


/**
 * This class simulates two IM clients talking in OTR
 *
 * @author Can Tang <c24tang@gmail.com>
 */

import java.net.*;
import java.io.*;
import ca.uwaterloo.crysp.otr.*;

import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.uwaterloo.crysp.otr.iface.*;

public class MsgClient {

	private static ExecutorService executor;
	final static int portNumber = 53617;//Set to port 0 if you don't know a specific open port
	final static int MAXNAMELENGTH = 16;//keep consistent with server, first message relies on it. 
	final static int MINNAMELEGTH = 3;
	static Scanner consoleIn = null;
	static boolean incommingConnectionRequest = false;
	static String connectedToUser = null;
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws OTRException 
	 */
	public static void main(String[] args) throws IOException, OTRException {
		executor = Executors.newCachedThreadPool();	
		System.out.println("Starting client.");
		System.out.print("Enter username: ");
		String username = getInputFromConsole(MINNAMELEGTH,MAXNAMELENGTH);

		// building the connection
		Socket client=new Socket(
				InetAddress.getLocalHost(),
				portNumber);
		
		//stream to destination
		BufferedOutputStream out = new BufferedOutputStream( client.getOutputStream() );
		out.write(Charset.forName("UTF-8").encode(username).array());
		out.flush();
		
		BufferedInputStream in = new BufferedInputStream( client.getInputStream() );
		int size = 4;
		byte[] data = new byte[size];
		in.read(data,0,size);
		String mes = Charset.forName("UTF-8").decode(ByteBuffer.wrap(data)).toString();
		System.out.printf("Server mess: %s\n",mes);
		
		executor.execute(new lobbyOutputToServer(in,out));//TODO: clean up streams at some point
		executor.execute(new lobbyInFromServer(in,out));
		
	}		

		
		/*BufferedReader in=new BufferedReader(new InputStreamReader(client.getInputStream()));
		BufferedReader in2=new BufferedReader(new InputStreamReader(System.in));
		System.out.println("\033[31mConnected to Server\033[0m");
		
		// Generate the keys
		OTRInterface bob = new UserState(new ca.uwaterloo.crysp.otr.crypt.jca.JCAProvider());		
		OTRCallbacks callback = new LocalCallback(client);

		// Send and receive the message repeatedly
		new SendingThread(in2, bob, callSign, "none", "alice", callback).start();
		new ReceivingThread(in, bob, "bob", "none", "alice", callback).start();
			

		System.out.println("\033[0m");*
		
		
		
//requestsFromServer
 * Read connection request
 * print to console who wants to connect(y/n)

/*console read thread
 * accept input from console
 * once input is taken
 * send to server
 */
	
	
/*
 * Lobby threads used before connection to specific user
 * 
 */
private static class lobbyOutputToServer implements Runnable
{
	private final BufferedInputStream inStream;
	private final BufferedOutputStream outStream;
	
	lobbyOutputToServer(BufferedInputStream inStream,BufferedOutputStream outStream)
	{
		this.inStream = inStream;
		this.outStream = outStream;
	}
	
	@Override
	public void run() {
		try {
			while(true)//TODO: Change this
			{
				System.out.print("Enter cmd: ");
				String cmd = getInputFromConsole(1,MAXNAMELENGTH);	
				outStream.write(Charset.forName("UTF-8").encode(cmd).array());
				outStream.flush();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

/*
 * Lobby threads used before connection to specific user
 * 
 */
private static class lobbyInFromServer implements Runnable
{
	private final BufferedInputStream inStream;
	private final BufferedOutputStream outStream;
	
	lobbyInFromServer(BufferedInputStream inStream,BufferedOutputStream outStream)
	{
		this.inStream = inStream;
		this.outStream = outStream;
	}
	
	@Override
	public void run() {
		try {
			while(true)//TODO:change this
			{
				byte[] data = new byte[MAXNAMELENGTH];
				inStream.read(data,0,MAXNAMELENGTH);	
				
				char[] cmd = Charset.forName("UTF-8").decode(ByteBuffer.wrap(data)).array();
				System.out.println("\n Server: "+new String(cmd));
				if(cmd[0] == '/' )
				{
					if(cmd[1] == 'c')
					{
						Pattern p = Pattern.compile("(?:\"(?<name>.+)\"){1}?");
						Matcher m = p.matcher(new String(cmd));
						if(m.find())
						{
							incommingConnectionRequest = true;
							System.out.println(m.group("name"));
							String name = m.group("name");			
							connectedToUser = name;
							System.out.printf("Incomming connection request from %s.\n",name);
							System.out.print("Accept (y/n): ");
						}
					}
				}
				else
				{
					
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}




/*
 * gets input from console 
 */
static String getInputFromConsole(int minLength,int maxLength)
{
	String input = null;
	if(consoleIn == null)
	{
		consoleIn = new Scanner(System.in);
	}
	boolean valid = true;
	do
	{
		input = consoleIn.nextLine();
		if( input.length() < minLength || input.length() > maxLength )
		{
			valid = false;
			System.out.println("Invalid ");
		}
		
	}while(!valid);
	return input;
}

class SendingThread extends Thread{
	private BufferedReader in;
	private OTRContext conn;
	private OTRCallbacks callback;
	private OTRInterface us;
	private String accountname;
	private String protocol;
	private String recipient;
	
	public SendingThread(BufferedReader in, OTRInterface us, String accName,
			String prot, String recName, OTRCallbacks callbacks){
		this.in=in;
		this.us=us;
		this.accountname = accName;
		this.protocol = prot;
		this.recipient = recName;
		this.conn=us.getContext(accName, prot, recName);
		this.callback = callbacks;
	}
	
	public void run(){
		String str;
		while(true){
			try {
				str = in.readLine();
				if(str.startsWith("/isq")){
					System.out.println("Please input the question");
					String question = in.readLine();
					System.out.println("Please input the secret");
					str = in.readLine();
					conn.initiateSmp_q(question, str, callback);
				}else if(str.startsWith("/is")){
					System.out.println("Please input the secret");
					str = in.readLine();
					conn.initiateSmp(str, callback);
				}else if(str.startsWith("/rs")){
					System.out.println("Please input the secret");
					str = in.readLine();
					conn.respondSmp(str, callback);
				}else if(str.startsWith("/as")){
					conn.abortSmp(callback);
				}else if(str.startsWith("/disc")){
					conn.disconnect(callback);
				}
				else{
					System.out.println("\033[31mTo OTR:"+str.length()+":\033[0m"+str);
					OTRTLV[] tlvs = new OTRTLV[1];
					tlvs[0]=new TLV(9, "TestTLV".getBytes());
					us.messageSending(accountname, protocol, recipient,
							str, tlvs, Policy.FRAGMENT_SEND_ALL, callback);
					/*if(str.length()!=0){
						System.out.println("\033[31mTo network:"+str.length()+":\033[35m"+str+"\033[0m");
						conn.fragmentAndSend(str,  callback);
					}*/
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
	}
}

class ReceivingThread extends Thread{
	private BufferedReader in;
	private OTRInterface us;
	private String accountname;
	private String protocol;
	private String sender;
	private OTRContext conn;
	private OTRCallbacks callback;
	
	public ReceivingThread(BufferedReader in, OTRInterface us, String accName,
			String prot, String sendName, OTRCallbacks callbacks){
		this.in=in;
		this.us=us;
		this.accountname = accName;
		this.protocol = prot;
		this.sender = sendName;
		this.conn=us.getContext(accName, prot, sendName);
		this.callback = callbacks;
	}
	
	public void run(){
		String res;
		while(true){
			try {
				res=in.readLine();
				System.out.println("\033[31mFrom network:"+res.length()+":\033[35m"+res+"\033[0m");
				StringTLV stlv = us.messageReceiving(accountname, protocol, sender, res, callback);
				if(stlv!=null){
					res=stlv.msg;
					System.out.println("\033[31mFrom OTR:"+res.length()+":\033[0m"+res);
				}
			} catch (SocketException e) {
				return;
			}
			catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
	}
}

class LocalCallback implements OTRCallbacks{
	
	Socket soc;
	PrintWriter out;
	
	public LocalCallback(Socket sock) throws IOException{
		soc=sock;
		out=new PrintWriter(soc.getOutputStream());
	}

	public void injectMessage(String accName, String prot, String rec, String msg){
		if(msg==null)return;
		System.out.println("\033[31mInjecting message to the recipient:"
				+msg.length()+":\033[35m"+msg+"\033[0m");
		out.println(msg);
		out.flush();
	}

	public int getOtrPolicy(OTRContext conn) {
		return Policy.DEFAULT;
	}

	public void goneSecure(OTRContext context) {
		System.out.println("\033[31mAKE succeeded\033[0m");
	}

	public int isLoggedIn(String accountname, String protocol,
			String recipient) {
		return 1;
	}

	public int maxMessageSize(OTRContext context) {
		return 1000;
	}

	public void newFingerprint(OTRInterface us,
			String accountname, String protocol, String username,
			byte[] fingerprint) {
		System.out.println("\033[31mNew fingerprint is created.\033[0m");
	}

	public void stillSecure(OTRContext context, int is_reply) {
		System.out.println("\033[31mStill secure.\033[0m");
	}

	public void updateContextList() {
		System.out.println("\033[31mUpdating context list.\033[0m");
	}

	public void writeFingerprints() {
		System.out.println("\033[31mWriting fingerprints.\033[0m");
	}

	public String errorMessage(OTRContext context, int err_code) {
		if(err_code==OTRCallbacks.OTRL_ERRCODE_MSG_NOT_IN_PRIVATE){
			return "You sent an encrypted message, but we finished" +
					"the private conversation.";
		}
		return null;
	}

	public void handleMsgEvent(int msg_event,
			OTRContext context, String message) {
		if(msg_event==OTRCallbacks.OTRL_MSGEVENT_CONNECTION_ENDED){
			System.out.println("\033[31mThe private connection has already ended.\033[0m");
		}else if(msg_event==OTRCallbacks.OTRL_MSGEVENT_RCVDMSG_NOT_IN_PRIVATE){
			System.out.println("\033[31mWe received an encrypted message, but we are not in" +
					"encryption state.\033[0m");
		}
	}

	public void handleSmpEvent(int smpEvent,
			OTRContext context, int progress_percent, String question) {
		if(smpEvent == OTRCallbacks.OTRL_SMPEVENT_ASK_FOR_SECRET){
			System.out.println("\033[31mThe other side has initialized SMP." +
					" Please respond with /rs.\033[0m");
		}else if(smpEvent == OTRCallbacks.OTRL_SMPEVENT_ASK_FOR_ANSWER){
			System.out.println("\033[31mThe other side has initialized SMP, with question:" +
					question + ", "+
			" Please respond with /rs.\033[0m");
		}else if(smpEvent == OTRCallbacks.OTRL_SMPEVENT_SUCCESS){
			System.out.println("\033[31mSMP succeeded.\033[0m");
		}else if(smpEvent == OTRCallbacks.OTRL_SMPEVENT_FAILURE){
			System.out.println("\033[31mSMP failed.\033[0m");
		}
		
		
	}
	
}
}