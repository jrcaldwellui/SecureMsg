import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
				byte[] data = new byte[MsgServer.MAXNAMELENGTH];	   
				user.getInStream().read(data,0,MsgServer.MAXNAMELENGTH);
				char[] msg = MsgServer.removeZeroBytesFromEnd(data).toCharArray();
				System.out.println(user.getUsername()+": "+new String(msg));
				
				if(msg[0] == '/' )//msg is cmd
				{
					handleCommand(msg);
				}else if(user.getInSessionWith() != null)//msg is 
				{
					System.out.println(user.getUsername()+" fwding mess to "+ user.getInSessionWith().getUsername());
					user.getInSessionWith().getOutStream().write(data);
					user.getInSessionWith().getOutStream().flush();
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
			user.getFutureOfThread().cancel(true);//stops user's thread
			user.leaveCurrentSession();
			MsgServer.removeUserFromLobby(user);
			user.Disconnect();
		}
	}
	
	
	/*
	 * manages all actions 
	 */
	private void handleCommand(char[] cmd) throws IOException
	{
		if(cmd[1] == 'c')//TODO: clean bloated chunk of code
		{
			Pattern p = Pattern.compile("(?:\"(?<name>.+)\"){1}?");
			Matcher m = p.matcher(new String(cmd));
			while(m.find())
			{
				String name = m.group("name");
				System.out.println(user.getUsername() + " requesting connection to: "+name);
				User userToConnTo = MsgServer.isUserInLobby(name);
				if(userToConnTo != null)
				{
					if(userToConnTo.getWaitingForSessionConfirmationFrom() == user )
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
								userToConnTo.getReadingThread().wait((long)30000);
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
		}else if(cmd[1]== 'd')
		{
			System.out.println("Leaving current session");
			user.leaveCurrentSession();
		}
		
		
	}
}
