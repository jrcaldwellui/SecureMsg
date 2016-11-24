import java.io.*;


/*
 * Contain all info about user and communicating with user
 * 
 */
public class User 
{
	private final String userName;
	private final BufferedInputStream inStream;
	private final BufferedOutputStream outStream;
	private final int timeConnected;
	private boolean connected;
	
	User(String userName, BufferedInputStream inStream , BufferedOutputStream outStream, int timeConnected)
	{
		this.userName = userName;
		this.inStream = inStream;
		this.outStream = outStream;
		this.timeConnected = timeConnected;
		connected = true;
	}
	
	/*
	 * Call to clean up streams affiliated with user, do not continue to use User after disconnect is called.
	 */
	public void Disconnect()
	{
		try {
			inStream.close();
		} catch (IOException e) {
			System.err.printf("Issue closing %s's input stream\n",userName);
			e.printStackTrace();
		}
		try {
			outStream.close();
		} catch (IOException e) {
			System.err.printf("Issue closing %s's output stream\n",userName);
			e.printStackTrace();
		}
		connected = false;
	}
	
	
	
}
