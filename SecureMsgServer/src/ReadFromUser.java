import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*
 * Reads from user handles cmds sent by user
 * if chat session has started with another user all msg data is directly fwded
 */
public class ReadFromUser implements Runnable{
	User user;
	
	ReadFromUser( User user)
	{
		this.user = user;
	}
	
	@Override
	public void run() {
		try
		{
			while(user.isConnected())
			{
				byte[] data = new byte[MsgServer.MAXMESSAGEBYTES];	   
				int connectionStatus = user.getInStream().read(data,0,MsgServer.MAXMESSAGEBYTES);
				String msg = MsgServer.getStringFromRawData(data);
				System.out.println(user.getUsername()+": "+new String(msg));
				
				if(connectionStatus != -1)
				{
					if(msg.startsWith("/"))//msg is cmd
					{
						handleCommand(msg);
					}else if(user.getInSessionWith() != null)//msg is 
					{
						System.out.println(user.getUsername()+" fwding mess to "+ user.getInSessionWith().getUsername());
						user.getInSessionWith().getOutStream().write(data);
						user.getInSessionWith().getOutStream().flush();
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
