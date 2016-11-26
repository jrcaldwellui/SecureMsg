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


/*
 * Users connected to server are in lobby
 * Users talking with another are in session
 */
public class MsgServer {

	private static ExecutorService executor;
	final static String hostName = "127.0.0.1"; // Localhost for testing
	final static int portNumber = 53617;//Set to port 0 if you don't know a specific open port
	final static int MAXNAMELENGTH = 16;//keep consistent with client, first message relies on it. 
	static long serverCreationTime;
	static ArrayList<User> usersInLobby= new ArrayList<User>();
	
	public static void main(String[] args) throws IOException {
		executor = Executors.newCachedThreadPool();	
		serverCreationTime = System.nanoTime();
		
	     try ( ServerSocket Server = new ServerSocket(portNumber)) 
	     {
	    	System.out.printf("Server is listening on port %d\n",Server.getLocalPort());
    		boolean waitForConnection = true;
    		int i = 0;
    		while(waitForConnection)
    		{
    			//Server.setSoTimeout(180*1000);//2 min timeout during server.accept
    			System.out.print("server awaiting connection\n");
    			Socket newSocket = Server.accept();
    			System.out.println("Connection recieved.");
    			BufferedInputStream inStream = null;
    			BufferedOutputStream outStream = null;

    			inStream = new BufferedInputStream( newSocket.getInputStream() );

    			byte[] data = new byte[MAXNAMELENGTH];	    			
    			inStream.read(data,0,MAXNAMELENGTH);
    			String callSign = removeZeroBytesFromEnd(data);
    			
    			outStream = new BufferedOutputStream( newSocket.getOutputStream() );
    			outStream.write( Charset.forName("UTF-8").encode("Conn").array() );
    			outStream.flush();
    			
    			if( !callSign.isEmpty() )
    			{
    				System.out.printf("%s connected\n",callSign);
    				addUserToLobby(new User(callSign,inStream,outStream,secElapsedSinceServerStart()) );
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
	 *  removes zero entries from end of byte array
	 */
	public static String removeZeroBytesFromEnd(byte[] data)
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
		user.setFutureOfThread( executor.submit(lobbyThread) );
	}
	
	public static void removeUserFromLobby(User user)
	{
		usersInLobby.remove(user);
		user.Disconnect();
	}
	
	/*
	 * returns: 
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
