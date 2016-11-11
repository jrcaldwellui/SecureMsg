import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MsgServer {

	private static ExecutorService executor;
	
	final static String hostName = "127.0.0.1"; // Localhost for testing
	final static int portNumber = 53617;//Set to port 0 if you don't know a specific open port
	static float serverCreationTime;
	
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
    			BufferedReader in = new BufferedReader(new InputStreamReader(newSocket.getInputStream()));
    			String callSign = in.readLine();
    			
    			if(!callSign.isEmpty())
    			{
    				System.out.printf("%s connected\n",callSign);
    				//readerFromUser(in);
    			}else
    			{
    				System.err.println("Invalid Callsign.\n");
    			}
    			waitForConnection = false;
    		} 	 
	    	 
	     }catch (java.net.SocketTimeoutException e)
	     {
	    	 System.err.println("Server timedout.");
		     System.exit(-1);  	 
	     }  catch (IOException e) {
	        System.err.println("Could not listen on port " + portNumber);
	        System.exit(-1);
	     } 
		
		executor.shutdown();
	}
	

	/*
	 * new thread for each user
	 */	
	private static class writerToUser implements Runnable{
		writerToUser(Socket pcSocket, BufferedReader in)
		{
			this.pcSocket = pcSocket;
			this.inFromPC = in;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	/*
	 * Will take messages from user and write them to who they are connected to
	 */
	private static class ReaderFromUser implements Runnable{
		BufferedReader inFromUser;
		ReaderFromUser(BufferedReader in)
		{
			this.pcSocket = pcSocket;
			this.inFromPC = in;
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			
		}
		
	}

	
	/*
	 * returns seconds since server's creation
	 */
	private static float secElapsedSinceServerStart()
	{
		long elapsedTimeNano = System.nanoTime()- serverCreationTime;
		long elapsedTimeMilli = TimeUnit.NANOSECONDS.toMillis(elapsedTimeNano);
		return elapsedTimeMilli/1000.0f;
	}
	
	/**
	 * Temporary function for random values
	 */
	void randomData() {
		int i;
		for (i=0; i<300; i++) {
			// data must be >=10 && <=50
			data[i] = (int) (10 + Math.random()*40);
		}
	}
	
	
}
