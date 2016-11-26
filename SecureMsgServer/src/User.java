import java.io.*;
import java.util.concurrent.Future;



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
	private User inSessionWith = null;
	private User waitingForSessionConfirmationFrom = null;
	public User getWaitingForSessionConfirmationFrom() {
		return waitingForSessionConfirmationFrom;
	}
	public void setWaitingForSessionConfirmationFrom(User waitingForSessionConfirmationFrom) {
		this.waitingForSessionConfirmationFrom = waitingForSessionConfirmationFrom;
	}

	private ReadFromUser readingThread = null;
	private Future<?> futureOfThread = null;//Used to cancel thread
	
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
	public User getInSessionWith() {
		return inSessionWith;
	}
	
	public void startChatSessionWith(User user) {
		this.inSessionWith = user;
	}
	
	/*
	 * Dont call if you want both users to end session call endSession
	 */
	public void leaveCurrentSession()
	{
		waitingForSessionConfirmationFrom = null;
		inSessionWith = null;
	}
	
	/*
	 * Makes both users leave their session.
	 */
	public void endSession()
	{
		inSessionWith.leaveCurrentSession();
		leaveCurrentSession();
	}
	
	public Future<?> getFutureOfThread() {
		return futureOfThread;
	}
	public void setFutureOfThread(Future<?> futureOfThread) {
		this.futureOfThread = futureOfThread;
	}
	
}
