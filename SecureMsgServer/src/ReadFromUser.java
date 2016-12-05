import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ca.uwaterloo.crysp.otr.TLV;
import ca.uwaterloo.crysp.otr.iface.OTRCallbacks;
import ca.uwaterloo.crysp.otr.iface.OTRContext;
import ca.uwaterloo.crysp.otr.iface.OTRInterface;
import ca.uwaterloo.crysp.otr.iface.OTRTLV;
import ca.uwaterloo.crysp.otr.iface.Policy;
import ca.uwaterloo.crysp.otr.iface.StringTLV;


/*
 * Reads from user handles cmds sent by user
 * if chat session has started with another user all msg data is directly fwded
 */
public class ReadFromUser implements Runnable{
	User user;
	private final BufferedReader inStream;
	private OTRInterface us;
	private String protocol;
	private String serverName;
	private OTRContext conn;
	private OTRCallbacks callback;

	ReadFromUser(User user)
	{
		this.us=user.getInterface();
		this.protocol = "none";
		this.serverName = "Server";
		this.callback = user.getCallbacks();
		this.inStream = user.getInStream();
		this.user = user;
		this.conn=us.getContext(serverName, protocol, user.getUsername());
	}

	
	@Override
	public void run() {
		try
		{
			while(user.isConnected())
			{
				String msg = readMessage();
				System.out.println(user.getUsername()+": "+msg);

				if(msg != null)
				{
					if(msg.startsWith("/"))//msg is cmd
					{
						handleCommand(msg);
					}else if(user.getInSessionWith() != null)//msg is 
					{
						/*System.out.println(user.getUsername()+" fwding mess to "+ user.getInSessionWith().getUsername());
						user.getInSessionWith().getOutStream().write(data);
						user.getInSessionWith().getOutStream().flush();*/ //TODO: implement
					}
				}
				else
				{
					user.Disconnect();
				}
					
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			System.out.println(user.getUsername() +" disconnected from server.");
			user.endSession();
			MsgServer.removeUserFromLobby(user);
			user.Disconnect();
		}
	}
	
	
	/*
	 * manages all actions 
	 */
	private void handleCommand(String cmd) throws IOException
	{
		if(cmd.startsWith("/c"))//TODO: clean bloated chunk of code
		{
			String name = extractSubstringInParenthesis(cmd);
			if(name != null)
			{
				System.out.println(user.getUsername() + " requesting connection to: "+name);
				User userToConnTo = MsgServer.isUserInLobby(name);
				if(userToConnTo != null)
				{
					if(userToConnTo.getWaitingForSessionConfirmationFrom() == user )// check if userToConnTo is already waiting for connection to user
					{	
						System.out.println(userToConnTo.getUsername()+" already wating for connection, notifying their thread");
						user.startChatSessionWith(userToConnTo);
						synchronized(this)
						{
							this.notify();
						}
						
						
					}
					else
					{
						user.setWaitingForSessionConfirmationFrom(userToConnTo);
						userToConnTo.getOutStream().write( Charset.forName("UTF-8").encode("/c \""+user.getUsername()+"\"").array() );			
						userToConnTo.getOutStream().flush();
						try {
							synchronized(userToConnTo.getReadingThread())
							{
								userToConnTo.getReadingThread().wait((long)30000);//thread waits 30 sec for other user to confirm chat session
							}
	
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						
						user.setWaitingForSessionConfirmationFrom(null);
						if(userToConnTo.getInSessionWith() == user)
						{
							System.out.println(userToConnTo.getUsername() +" confirmed conn starting session with, "+user.getUsername());
							user.startChatSessionWith(userToConnTo);
						}
						else
						{
							System.out.println(userToConnTo.getUsername()+" did not confirm conn" + user.getUsername() +" will not start session." );
						}
					}
				}else
				{
					System.err.println("No user with name "+name+" found in lobby.");
				}
			}
		}else if(cmd.startsWith("/d"))
		{
			System.out.println("Leaving current session");
			user.leaveCurrentSession();
		}
		


	}
	private String readMessage() throws Exception
	{
		String res=inStream.readLine();
		StringTLV stlv = us.messageReceiving(serverName, protocol, user.getUsername(), res, callback);
		if(stlv!=null){
			System.out.println("From network:"+res.length()+":"+res);
			return stlv.msg;
		}
		return "";
	}
	
	private void sendMessage(String msg) throws Exception
	{
		System.out.println("Sending: "+msg.length()+":"+msg);
		OTRTLV[] tlvs = new OTRTLV[1];
		tlvs[0]=new TLV(9, "TestTLV".getBytes());
		us.messageSending(user.getUsername(), protocol, serverName,
				msg, tlvs, Policy.FRAGMENT_SEND_ALL, callback);
	}
	/*
	 * Gets substring in parenthesis from string
	 * @param s string of format ..."infoToExtract"...
	 * @return substring in parenthesis or null if no parenthesis
	 */
	private String extractSubstringInParenthesis(String s)
	{
		Pattern p = Pattern.compile("(?:\"(?<name>.+)\"){1}?");
		Matcher m = p.matcher(s);
		if(m.find())
		{
			return m.group("name");
		}
		return null;
	}
}
