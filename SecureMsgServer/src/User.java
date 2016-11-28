import java.io.*;
import java.util.concurrent.Future;



/*
 * Contains all info about user and communicating with user
 * 
 */
public class User 
{


	private final String username;
	private final BufferedInputStream inStream;
	private final BufferedOutputStream outStream;
	private final int timeConnected;
	private boolean connected;
	private User inSessionWith = null;		//current chat session partner
	private User waitingForSessionConfirmationFrom = null; //if user requests session this will contain the user they are requesting from
	private ReadFromUser readingThread = null;	//thread server uses to read from this user
	

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

	

	/*
	 * Makes both users leave their session.
	 */
	public void endSession()
	{
		if(inSessionWith != null)
		{
			inSessionWith.leaveCurrentSession();
		}	
		leaveCurrentSession();
	}
	
	/*
	 * Don't call directly, call endSession if you want both users to end session 
	 */
	public void leaveCurrentSession()
	{
		waitingForSessionConfirmationFrom = null;
		inSessionWith = null;
	}
	
	
	//simple getter/setters
	public User getInSessionWith() {
		return inSessionWith;
	}
	
	public void startChatSessionWith(User user) {
		this.inSessionWith = user;
	}
	
	public User getWaitingForSessionConfirmationFrom() {
		return waitingForSessionConfirmationFrom;
	}
	public void setWaitingForSessionConfirmationFrom(User waitingForSessionConfirmationFrom) {
		this.waitingForSessionConfirmationFrom = waitingForSessionConfirmationFrom;
	}

	public String getUsername() {
		return username;
	}
	public BufferedInputStream getInStream() {
		return inStream;
	}
	public BufferedOutputStream getOutStream() {
		return outStream;
	}
	
	public ReadFromUser getReadingThread() {
		return readingThread;
	}
	public void setReadingThread(ReadFromUser readingThread) {
		this.readingThread = readingThread;
	}
	public boolean isConnected() {
		return connected;
	}
	public void setConnected(boolean connected) {
		this.connected = connected;
	}
	public int getTimeConnected() {
		return timeConnected;
	}
	
}
