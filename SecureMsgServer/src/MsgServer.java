import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import ca.uwaterloo.crysp.otr.UserState;
import ca.uwaterloo.crysp.otr.iface.OTRCallbacks;
import ca.uwaterloo.crysp.otr.iface.OTRContext;
import ca.uwaterloo.crysp.otr.iface.OTRInterface;
import ca.uwaterloo.crysp.otr.iface.Policy;



/*
 * Users connected to server are in lobby
 * Users talking with another are in session
 */
public class MsgServer {

	private static ExecutorService executor;
	final static String hostName = "127.0.0.1"; // Localhost for testing
	final static int portNumber = 53617;//Set to port 0 if you don't know a specific open port
	final static int MAXMESSAGEBYTES = 160;//keep consistent with client
	static long serverCreationTime;
	static ArrayList<User> usersInLobby= new ArrayList<User>();
	
	public static void main(String[] args) throws IOException {
		executor = Executors.newCachedThreadPool();	
		serverCreationTime = System.nanoTime();
		
	     try ( ServerSocket Server = new ServerSocket(portNumber)) 
	     {
	    	System.out.printf("Server is listening on port %d\n",Server.getLocalPort());
    		boolean waitForConnection = true;
    		while(waitForConnection)
    		{
    			//Server.setSoTimeout(180*1000);//2 min timeout during server.accept
    			System.out.print("server awaiting connection\n");
    			Socket newSocket = Server.accept();
    			System.out.println("Connection recieved.");

    			BufferedReader inStream = new BufferedReader(new InputStreamReader(newSocket.getInputStream()));
    			OTRInterface server = new UserState(new ca.uwaterloo.crysp.otr.crypt.jca.JCAProvider());
    			OTRCallbacks callback = new LocalCallback(newSocket);
    			String username = inStream.readLine().trim();//TODO:change to real value
    			if( true /*!callSign.isEmpty()*/ )
    			{
    				System.out.printf("%s connected\n",username);
    				addUserToLobby(new User(username,inStream,secElapsedSinceServerStart(),callback,server) );
    			}else
    			{
    				System.err.println("Invalid Callsign.\n");
    			}

    		} 	 
    		Iterator<User> uit = usersInLobby.iterator();
    		while(uit.hasNext())
    		{
    			System.out.printf("%s\n", uit.next().getUsername() );
    		}
	    	 
	     }catch (java.net.SocketTimeoutException e)
	     {
	    	 System.err.println("Server timedout.");
		     System.exit(-1);  	 
	     }  catch (IOException e) {
	        System.err.println("Could not listen on port " + portNumber);
	        System.exit(-1);
	     }finally
	     {
	    	 
	     }
		
		executor.shutdown();
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
			//System.out.printf("%d, ", numOfZeroBytesAtEnd);
		}
		//System.out.println("");
		int newSize = data.length-numOfZeroBytesAtEnd;//1-1=0 0,0
		byte[] newData = Arrays.copyOfRange(data, 0, newSize);
		String mess = Charset.forName("UTF-8").decode(ByteBuffer.wrap(newData)).toString();
		//System.out.println("Cleaned message: "+mess);
		return mess;
	}
	
	/*
	 * Adds user to lobby list, and executes new thread for reading cmds from user
	 */
	private static void addUserToLobby(User user)
	{
		usersInLobby.add(user);
		ReadFromUser lobbyThread = new ReadFromUser(user);
		user.setReadingThread(lobbyThread);
		executor.execute(lobbyThread);
	}
	
	public static void removeUserFromLobby(User user)
	{
		usersInLobby.remove(user);
		user.Disconnect();
	}
	
	/*
	 * @return user if they are in lobby, null if no user with name is in lobby
	 */
	static User isUserInLobby(String username)
	{
		Iterator<User> i = usersInLobby.iterator();
		while(i.hasNext())
		{
			User user = i.next();
			if(user.getUsername().equals(username))
			{
				return user;
			}
		}
		return null;
	}

	
	/*
	 * returns seconds since server's creation
	 */
	private static int secElapsedSinceServerStart()
	{
		long elapsedTimeNano = System.nanoTime()- serverCreationTime;
		long elapsedTimeMilli = TimeUnit.NANOSECONDS.toMillis(elapsedTimeNano);
		return (int)(elapsedTimeMilli/1000.0f);
	}
}


class LocalCallback implements OTRCallbacks
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

