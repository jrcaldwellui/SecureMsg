
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
	static String serverName = "Server";
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
		System.out.println("Starting client.");
		System.out.print("Enter username: ");
		username = getInputFromConsole(MINNAMELEGTH,MAXMESSAGEBYTES);
		client=new Socket(
				InetAddress.getLocalHost(),
				portNumber);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
		BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

		
		executor = Executors.newCachedThreadPool();	

			
		//OTR
		OTRInterface localUser = new UserState(new ca.uwaterloo.crysp.otr.crypt.jca.JCAProvider());
		LocalCallback callback = new LocalCallback(client);
		
		//TODO: confirm server connection before creating reading thread
		executor.execute(new writeToServer(localUser,username,"none",serverName, callback));//TODO: clean up streams at some point
		executor.execute(new readFromServer(in,localUser,username,"none",serverName, callback));
		executor.shutdown();

	}			

	/*
	 * Handles user cmds and msgs to the server
	 */
	private static class writeToServer implements Runnable
	{
		private OTRContext conn;
		private LocalCallback callback;
		private OTRInterface us;
		private String accountname;
		private String protocol;
		private String recipient;
		private boolean connected;
		
		writeToServer(OTRInterface us, String accName, String prot, String recName, LocalCallback callbacks)
		{
			//this.inStream = inStream;
			connected = true;
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
				sendMsgToServer(username, serverName);//TODO: checks if username fails or is invalid
				sendMsgToServer("", serverName);//start key exchange

				while(connected)
				{
					System.out.print("Enter cmd/msg (/h for help): ");
					String msg = getInputFromConsole(1,MAXMESSAGEBYTES);
					if(msg.startsWith("/"))
					{
						if( !tryToHandleCommandLocally(msg) )
						{
							sendMsgToServer(msg,serverName);
						}
					}
					else
					{
						if(connectedToUser != null)
						{
							sendMsgToServer(msg, connectedToUser);
						}
						else
						{
							System.out.println("Sending direct message to server.");
							sendMsgToServer(msg,serverName);
						}
						
					}
					
				}
			}catch (SocketException e) {
				e.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			catch(Exception e){
				e.printStackTrace();
			}
			finally
			{
				System.out.println("Write thread disconnected.");
			}
		}
		
		/*
		 * Handles cmds that can be dealt with locally
		 * Also does validation check on cmds going to server
		 * @return false if can't handle locally
		 */
		private boolean tryToHandleCommandLocally(String cmd) throws Exception
		{
			if(cmd.startsWith("/h"))
			{
				System.out.println("cmd list:");
				System.out.println("/l for list of connected users");
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
				}
				else
				{
					System.out.println("Invalid cmd, type /h for help.");
					return true;
				}
			}else if(cmd.startsWith("/d"))
			{
				connectedToUser = null;
				return false;
			}else if(cmd.startsWith("/e"))
			{
				connected = false;
				client.close();
				return true;
			}else if(cmd.startsWith("/isq")){ // The following SMP work: based on @author Can Tang <c24tang@gmail.com> library
				conn=us.getContext(username, protocol, connectedToUser);
				System.out.println("Please input the question");
				String question = getInputFromConsole(1,164);
				System.out.println("Please input the secret");
				String str = getInputFromConsole(1,164);
				conn.initiateSmp_q(question, str, callback);
				return true;
			}else if(cmd.startsWith("/is")){
				conn=us.getContext(username, protocol, connectedToUser);
				System.out.println("Please input the secret");
				String str = getInputFromConsole(1,164);
				conn.initiateSmp(str, callback);
				return true;
			}else if(cmd.startsWith("/rs")){
				conn=us.getContext(username, protocol, connectedToUser);
				System.out.println("Please input the secret");
				String str = getInputFromConsole(1,164);
				conn.respondSmp(str, callback);
				return true;
			}else if(cmd.startsWith("/as")){
				conn=us.getContext(username, protocol, connectedToUser);
				conn.abortSmp(callback);
				return true;
			}
			return false;
		}
		
		/*
		 * Sends messages based on otr, the command /d as plaintext to server
		 * @param msg String to send,
		 * @param recipientName, typically serverName or clientName
		 */
		private void sendMsgToServer(String msg, String recipientName) throws Exception
		{
			if(msg.startsWith("/d"))//plain text command
			{
				callback.out.println(msg);
				callback.out.flush();
			}
			else
			{
				System.out.println("Sending: "+msg.length()+":"+msg);
				String result =us.messageSending(accountname, protocol, recipientName,
						msg, null, Policy.FRAGMENT_SEND_ALL, callback);//TODO: fix so msg doesn't need fwd slash
				if(result != null)
				{
					System.out.println("Results: "+ result);
				}
			}

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
					if(msg != null)
					{
						System.out.println("From network:"+msg.length()+":"+msg);	
						StringTLV stlv;
						if(connectedToUser == null)
						{
							stlv = us.messageReceiving(accountname, protocol, serverName, msg, callback);
							
						}else
						{
							stlv = us.messageReceiving(accountname, protocol, connectedToUser, msg, callback);

						}
						
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
				String name = extractSubstringInParenthesis(cmd);
				if(name != null)
				{
					
					//Display info to user
					System.out.printf("Incomming connection request from %s.\n",name);
					System.out.println("To Accept type exactly:/c \""+name+"\"");
					System.out.print("Enter cmd/msg (/h for help): ");
				}
			}
			else if(cmd.startsWith("/success"))
			{
				String name = extractSubstringInParenthesis(cmd);
				if(name != null)
				{
					connectedToUser = name;
					System.out.println("Chat Session started with "+name);
				}
			}
			else if(cmd.startsWith("/l"))
			{
				System.out.println("All users: ");
				System.out.println(cmd);
			}
		}
		
		/*
		 * Gets substring in parenthesis from string
		 * @param s string of format ..."infoToExtract"...
		 * @return substring in parenthesis or null if no parenthesis
		 */
		private static String extractSubstringInParenthesis(String s)
		{
			Pattern p = Pattern.compile("(?:\"(?<name>.+)\"){1}?");
			Matcher m = p.matcher(s);
			if(m.find())
			{
				return m.group("name");
			}
			return null;
		}
		
		/*
		 * 
		 *  removes zero entries from end of byte array return UTF-8 rep string
		 *  @param data 
		 *  
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
	
	
}

/**
 * 
 * Provided by library
 * @author Can Tang <c24tang@gmail.com>
 */
class LocalCallback implements OTRCallbacks
{
	Socket soc;
	public PrintWriter out;
	
	public LocalCallback(Socket sock) throws IOException{
		soc=sock;
		out=new PrintWriter(soc.getOutputStream());
	}

	public void injectMessage(String accName, String prot, String rec, String msg){
		if(msg==null)return;
		System.out.println("Injecting message to the recipient:"
				+msg.length()+": "+msg+"");
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
		//System.out.println("Still secure.");
	}

	public void updateContextList() {
		//System.out.println("Updating context list.");
	}

	public void writeFingerprints() {
		//System.out.println("Writing fingerprints.");
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