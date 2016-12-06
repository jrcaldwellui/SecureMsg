import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
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
				String res=inStream.readLine();
				if(res != null && res.startsWith("/"))//plain text command from network
				{
					System.out.println("plaintext cmd from client");
					handleCommand(res);
				}
				else if(user.getInSessionWith() == null)//encrypted command from network
				{
					if(res != null)
					{
						String msg = interpretMessage(res);
						System.out.println(user.getUsername()+": OTR: "+msg);	
						
						if(msg != null)
						{
							if(msg.startsWith("/"))//msg is cmd
							{
								handleCommand(msg);
							}
						}
						else
						{
							user.Disconnect();
						}
					}
					else
					{
						user.Disconnect();
					}
				}
				else //message fwding over network
				{
					System.out.println(user.getUsername()+" fwding mess to "+ user.getInSessionWith().getUsername());
					PrintWriter partnersStream = user.getInSessionWith().getOutStream();
					partnersStream.println(res);
					partnersStream.flush();
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
	private void handleCommand(String cmd) throws Exception
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
						try
						{
							sendMessage(user,"/success \""+userToConnTo.getUsername()+"\"");
							sendMessage(userToConnTo,"/success \""+user.getUsername()+"\"");
						}catch(Exception e)
						{
							e.printStackTrace();//TODO: handle better, stop user session.
						}
						
					}
					else
					{
						try
						{
							user.setWaitingForSessionConfirmationFrom(userToConnTo);
							sendMessage(userToConnTo,"/c \""+user.getUsername()+"\"");
	
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
						}catch(Exception e)
						{
							e.printStackTrace();
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
			user.endSession();
		}
		else if(cmd.startsWith("/l"))
		{
			System.out.println("List cmd");
			String allUsers = "/l ";
			Iterator<User> it = MsgServer.usersInLobby.iterator();
			while(it.hasNext())
			{
				allUsers = allUsers + it.next().getUsername() + " ";
			}
			System.out.println(allUsers);
			sendMessage(user,allUsers);
		}
		
	}
	
	private String interpretMessage(String rawMessage) throws Exception
	{

		StringTLV stlv = MsgServer.serverInterface.messageReceiving(user.getUsername(), protocol, serverName, rawMessage, callback);
		if(stlv!=null){
			System.out.println("Raw from network:"+rawMessage.length()+":"+rawMessage);
			return stlv.msg;
		}
		return "";
	}
	
	private void sendMessage(User recipient,String msg) throws Exception
	{
		conn=MsgServer.serverInterface.getContext(serverName, protocol, recipient.getUsername());
		System.out.println("Sending to "+recipient.getUsername()+": "+msg.length()+":"+msg);
		OTRTLV[] tlvs = new OTRTLV[1];
		tlvs[0]=new TLV(9, "TestTLV".getBytes());
		MsgServer.serverInterface.messageSending(serverName, protocol, recipient.getUsername(),
				msg, null, Policy.FRAGMENT_SEND_ALL, recipient.getCallbacks() );
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
