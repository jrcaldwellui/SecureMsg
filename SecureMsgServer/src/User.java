import java.io.*;


/*
 * Contain all info about user and communicating with user
 * 
 */
public class User 
{
	public String getUsername() {
		return username;
	}
	public BufferedInputStream getInStream() {
		return inStream;
	}
	public BufferedOutputStream getOutStream() {
		return outStream;
	}

	private final String username;
	private final BufferedInputStream inStream;
	private final BufferedOutputStream outStream;
	private final int timeConnected;
	private boolean connected;
	
	public boolean isConnected() {
		return connected;
	}
	public void setConnected(boolean connected) {
		this.connected = connected;
	}
	public int getTimeConnected() {
		return timeConnected;
	}
	User(String userName, BufferedInputStream inStream , BufferedOutputStream outStream, int timeConnected)
	{
		this.username = userName;
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
			System.err.printf("Issue closing %s's input stream\n",username);
			e.printStackTrace();
		}
		try {
			outStream.close();
		} catch (IOException e) {
			System.err.printf("Issue closing %s's output stream\n",username);
			e.printStackTrace();
		}
		connected = false;
	}
	
	
	
}
