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
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.uwaterloo.crysp.otr.iface.*;

public class MsgClient {

	private static ExecutorService executor;
	final static int portNumber = 53617;//Set to port 0 if you don't know a specific open port
	final static int MAXMESSAGEBYTES = 160;//keep consistent with server
	final static int MINNAMELEGTH = 3;
	static Scanner consoleIn = null;
	static boolean incommingConnectionRequest = false;
	static String connectedToUser = null; //name who client is concected to
	static Socket client = null;
	static String username = "";
	/**
	 * 
	 * Set up threads for writing and reading to server
	 * 
	 * @param args
	 * @throws IOException 
	 * @throws OTRException 
	 */
	public static void main(String[] args) throws IOException, OTRException {
		// building the connection
		client=new Socket(
				InetAddress.getLocalHost(),
				portNumber);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));

		BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

		
		executor = Executors.newCachedThreadPool();	
		System.out.println("Starting client.");
		System.out.print("Enter username: ");
		username = getInputFromConsole(MINNAMELEGTH,MAXMESSAGEBYTES);
		
		//OTR
		OTRInterface localUser = new UserState(new ca.uwaterloo.crysp.otr.crypt.jca.JCAProvider());
		OTRCallbacks callback = new LocalCallback(client);
		
		//TODO: confirm server connection before creating reading thread
		executor.execute(new writeToServer(localUser,username,"none","server", callback));//TODO: clean up streams at some point
		executor.execute(new readFromServer(in,localUser,username,"none","server", callback));
		executor.shutdown();

	}			

	/*
	 * Handles user cmds and msgs to the server
	 */
	private static class writeToServer implements Runnable
	{
		private OTRContext conn;
		private OTRCallbacks callback;
		private OTRInterface us;
		private String accountname;
		private String protocol;
		private String recipient;
		
		writeToServer(OTRInterface us, String accName, String prot, String recName, OTRCallbacks callbacks)
		{
			//this.inStream = inStream;
			this.us=us;
			this.accountname = accName;
			this.protocol = prot;
			this.recipient = recName;
			this.conn=us.getContext(accName, prot, recName);
			this.callback = callbacks;
		}

		
		@Override
		public void run() {
			try {
				sendMsgToServer(username);//TODO: checks if username fails or is invalid

				while(true)
				{
					System.out.print("Enter cmd/msg (/h for help): ");
					String msg = getInputFromConsole(1,MAXMESSAGEBYTES);
					if(msg.startsWith("/"))
					{
						if( !tryToHandleCommandLocally(msg) )
						{
							sendMsgToServer(msg);
						}
					}
					else
					{
						sendMsgToServer(msg);
					}
					
				}
			}catch (SocketException e) {
				System.out.println("Write thread disconnected.");
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		
		/*
		 * Handles cmds that can be dealt with locally
		 * Also does validation check on cmds going to server
		 * @return false if can't handle locally
		 */
		private boolean tryToHandleCommandLocally(String cmd) throws IOException
		{
			if(cmd.startsWith("/h"))
			{
				System.out.println("cmd list:");
				System.out.println("/e disconnect from server");
				System.out.println("/c \"username\" connect to user" );
				System.out.println("/d disconnect from user");
				return true;
			}else if(cmd.startsWith("/c"))
			{
				Pattern p = Pattern.compile("(?:\"(?<name>.+)\"){1}?");//TODO: change this copy paste patern code to method 
				Matcher m = p.matcher(cmd);
				if(m.find())
				{
					incommingConnectionRequest = true;
					String name = m.group("name");
					connectedToUser = name;
				}
				else
				{
					System.out.println("Invalid cmd, type /h for help.");
					return true;
				}
			}else if(cmd.startsWith("/e"))
			{
				client.close();
			}
			
			return false;
		}
		
		/*
		 * @param msg String to send
		 */
		private void sendMsgToServer(String msg) throws Exception
		{
			System.out.println("Sending: "+msg.length()+":"+msg);
			OTRTLV[] tlvs = new OTRTLV[1];
			tlvs[0]=new TLV(9, "TestTLV".getBytes());
			us.messageSending(accountname, protocol, recipient,
					msg, tlvs, Policy.FRAGMENT_SEND_ALL, callback);

		}
	}
	
	
	/*
	 * Reads from server displaying messages and handles cmds that come from server
	 */
	private static class readFromServer implements Runnable
	{
		private final BufferedReader inStream;
		private OTRInterface us;
		private String accountname;
		private String protocol;
		private String sender;
		private OTRContext conn;
		private OTRCallbacks callback;
	
		readFromServer(BufferedReader inStream,OTRInterface us, String accName,
				String prot, String sendName, OTRCallbacks callbacks){
			this.us=us;
			this.accountname = accName;
			this.protocol = prot;
			this.sender = sendName;
			this.conn=us.getContext(accName, prot, sendName);
			this.callback = callbacks;
			this.inStream = inStream;
	
		}

		
		@Override
		public void run() {
			try {
				while(true)
				{
					String msg = inStream.readLine();
					System.out.println("From network:"+msg.length()+":"+msg);
					StringTLV stlv = us.messageReceiving(accountname, protocol, sender, msg, callback);
					if(stlv!=null)
					{
						msg=stlv.msg;
						System.out.println("From OTR:"+msg.length()+":"+msg);

			
						//check if cmd
						if(msg.startsWith("/"))
						{
							System.out.println("\nServer: "+new String(msg));
							handleCommandFromServer(msg);
						}
						else
						{
							System.out.println("\n"+connectedToUser+": "+new String(msg));
							System.out.print("Enter cmd/msg: ");
						}
					}
				}
			}catch (SocketException e){
				System.out.println("\nRead thread disconnected.");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e)
			{
				e.printStackTrace();
			}

		}
		
		/*
		 * Handle actions associated with cmds the server may send user
		 */
		private void handleCommandFromServer(String cmd)
		{
			if(cmd.startsWith("/c"))// connect to user
			{
				//finds name surrounded by parenthesis
				Pattern p = Pattern.compile("(?:\"(?<name>.+)\"){1}?");
				Matcher m = p.matcher(new String(cmd));
				if(m.find())
				{
					incommingConnectionRequest = true;
					String name = m.group("name");			
					connectedToUser = name;
					
					//Display info to user
					System.out.printf("Incomming connection request from %s.\n",name);
					System.out.println("To Accept type exactly:/c \""+name+"\"");
					System.out.print("Enter cmd/msg (/h for help): ");
				}
			}
		}
		
		/*
		 *  removes zero entries from end of byte array return UTF-8 rep string
		 */
		public static String getStringFromRawData(byte[] data)
		{
			int numOfZeroBytesAtEnd = 0;
			for(int i=0;i<data.length;i++)
			{
				if(data[i] == 0)
				{
					numOfZeroBytesAtEnd++;
				}
				else
				{
					numOfZeroBytesAtEnd=0;
				}
			}
			int newSize = data.length-numOfZeroBytesAtEnd;//1-1=0 0,0
			byte[] newData = Arrays.copyOfRange(data, 0, newSize);
			String mess = Charset.forName("UTF-8").decode(ByteBuffer.wrap(newData)).toString();
			return mess;
		}
		
	}

	/*
	 * Gets input from console 
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
	
	public static class LocalCallback implements OTRCallbacks
	{
		Socket soc;
		PrintWriter out;
		
		public LocalCallback(Socket sock) throws IOException{
			soc=sock;
			out=new PrintWriter(soc.getOutputStream());
		}
	
		public void injectMessage(String accName, String prot, String rec, String msg){
			if(msg==null)return;
			System.out.println("Injecting message to the recipient:"
					+msg.length()+":\033[35m"+msg+"");
			out.println(msg);
			out.flush();
		}
	
		public int getOtrPolicy(OTRContext conn) {
			return Policy.DEFAULT;
		}
	
		public void goneSecure(OTRContext context) {
			System.out.println("AKE succeeded");
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
			System.out.println("New fingerprint is created.");
		}
	
		public void stillSecure(OTRContext context, int is_reply) {
			System.out.println("Still secure.");
		}
	
		public void updateContextList() {
			System.out.println("Updating context list.");
		}
	
		public void writeFingerprints() {
			System.out.println("Writing fingerprints.");
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
				System.out.println("The private connection has already ended.");
			}else if(msg_event==OTRCallbacks.OTRL_MSGEVENT_RCVDMSG_NOT_IN_PRIVATE){
				System.out.println("We received an encrypted message, but we are not in" +
						"encryption state.");
			}
		}
	
		public void handleSmpEvent(int smpEvent,
				OTRContext context, int progress_percent, String question) {
			if(smpEvent == OTRCallbacks.OTRL_SMPEVENT_ASK_FOR_SECRET){
				System.out.println("The other side has initialized SMP." +
						" Please respond with /rs.");
			}else if(smpEvent == OTRCallbacks.OTRL_SMPEVENT_ASK_FOR_ANSWER){
				System.out.println("The other side has initialized SMP, with question:" +
						question + ", "+
				" Please respond with /rs.");
			}else if(smpEvent == OTRCallbacks.OTRL_SMPEVENT_SUCCESS){
				System.out.println("SMP succeeded.");
			}else if(smpEvent == OTRCallbacks.OTRL_SMPEVENT_FAILURE){
				System.out.println("SMP failed.");
			}	
		}
	}


}