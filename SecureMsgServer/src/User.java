import java.io.*;
import java.util.concurrent.Future;
import ca.uwaterloo.crysp.otr.TLV;
import ca.uwaterloo.crysp.otr.iface.OTRCallbacks;
import ca.uwaterloo.crysp.otr.iface.OTRContext;
import ca.uwaterloo.crysp.otr.iface.OTRInterface;
import ca.uwaterloo.crysp.otr.iface.OTRTLV;
import ca.uwaterloo.crysp.otr.iface.Policy;
import ca.uwaterloo.crysp.otr.iface.StringTLV;



/*
 * Contains all info about user and communicating with user
 * 
 */
public class User 
{


	private final String username;
	private final BufferedReader inStream;
	private final PrintWriter outStream;
	private final int timeConnected;
	private boolean connected;
	private User inSessionWith = null;		//current chat session partner
	private User waitingForSessionConfirmationFrom = null; //if user requests session this will contain the user they are requesting from
	private ReadFromUser readingThread = null;	//thread server uses to read from this user
	private final OTRInterface us;
	
	
	public OTRInterface getInterface() {
		return us;
	}
	public OTRCallbacks getCallbacks() {
		return callbacks;
	}

	private final OTRCallbacks callbacks;

	User(String userName, BufferedReader inStream, PrintWriter outStream , int timeConnected, OTRCallbacks callbacks, OTRInterface us)
	{
		this.username = userName;
		this.inStream = inStream;
		this.callbacks = callbacks;
		this.us = us;
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
		outStream.close();
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
	public BufferedReader getInStream() {
		return inStream;
	}
	public PrintWriter getOutStream() {
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
