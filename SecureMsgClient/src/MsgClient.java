/*
 *  Java OTR library
 *  Copyright (C) 2008-2009  Ian Goldberg, Muhaimeen Ashraf, Andrew Chung,
 *                           Can Tang
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of version 2.1 of the GNU Lesser General
 *  Public License as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


/**
 * This class simulates two IM clients talking in OTR
 *
 * @author Can Tang <c24tang@gmail.com>
 */

import java.net.*;
import java.io.*;
import ca.uwaterloo.crysp.otr.*;

import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.uwaterloo.crysp.otr.iface.*;

public class MsgClient {

	private static ExecutorService executor;
	final static int portNumber = 53617;//Set to port 0 if you don't know a specific open port
	final static int MAXMESSAGEBYTES = 160;//keep consistent with server
	final static int MINNAMELEGTH = 3;
	static Scanner consoleIn = null;
	static boolean incommingConnectionRequest = false;
	static String connectedToUser = null; //name who client is concected to
	static Socket client = null;
	/**
	 * 
	 * Set up threads for writing and reading to server
	 * 
	 * @param args
	 * @throws IOException 
	 * @throws OTRException 
	 */
	public static void main(String[] args) throws IOException, OTRException {
		executor = Executors.newCachedThreadPool();	
		System.out.println("Starting client.");
		System.out.print("Enter username: ");
		String username = getInputFromConsole(MINNAMELEGTH,MAXMESSAGEBYTES);

		// building the connection
		client=new Socket(
				InetAddress.getLocalHost(),
				portNumber);
		
		//stream to server
		BufferedOutputStream out = new BufferedOutputStream( client.getOutputStream() );
		out.write(Charset.forName("UTF-8").encode(username).array());
		out.flush();
		
		//stream from server
		BufferedInputStream in = new BufferedInputStream( client.getInputStream() );
		
		//Ack from server
		int size = 4;
		byte[] data = new byte[size];
		in.read(data,0,size);
		String mes = Charset.forName("UTF-8").decode(ByteBuffer.wrap(data)).toString();//this is where server acknolges connections, send conn
		System.out.printf("Server mess: %s\n",mes);
		
		//TODO: confirm server connection before creating reading thread
		executor.execute(new writeToServer(in,out));//TODO: clean up streams at some point
		executor.execute(new readFromServer(in,out));
		executor.shutdown();
	}			

	/*
	 * Handles user cmds and msgs to the server
	 */
	private static class writeToServer implements Runnable
	{
		private final BufferedInputStream inStream;
		private final BufferedOutputStream outStream;
		
		writeToServer(BufferedInputStream inStream,BufferedOutputStream outStream)
		{
			this.inStream = inStream;
			this.outStream = outStream;
		}
		
		@Override
		public void run() {
			try {
				while(true)
				{
					System.out.print("Enter cmd/msg (/h for help): ");
					String msg = getInputFromConsole(1,MAXMESSAGEBYTES);
					if(msg.startsWith("/"))
					{
						if( !tryToHandleCommandLocally(msg) )
						{
							sendMsgToServer(msg);
						}
					}
					else
					{
						sendMsgToServer(msg);
					}
					
				}
			}catch (SocketException e) {
				System.out.println("Write thread disconnected.");
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		/*
		 * Handles cmds that can be dealt with locally
		 * Also does validation check on cmds going to server
		 * @return false if can't handle locally
		 */
		private boolean tryToHandleCommandLocally(String cmd) throws IOException
		{
			if(cmd.startsWith("/h"))
			{
				System.out.println("cmd list:");
				System.out.println("/e disconnect from server");
				System.out.println("/c \"username\" connect to user" );
				System.out.println("/d disconnect from user");
				return true;
			}else if(cmd.startsWith("/c"))
			{
				Pattern p = Pattern.compile("(?:\"(?<name>.+)\"){1}?");//TODO: change this copy paste patern code to method 
				Matcher m = p.matcher(cmd);
				if(m.find())
				{
					incommingConnectionRequest = true;
					String name = m.group("name");
					connectedToUser = name;
				}
				else
				{
					System.out.println("Invalid cmd, type /h for help.");
					return true;
				}
			}else if(cmd.startsWith("/e"))
			{
				client.close();
			}
			
			return false;
		}
		
		/*
		 * encodes msg as UTF-8 and writes to server
		 * @param msg String to send
		 */
		private void sendMsgToServer(String msg) throws IOException
		{
			//TODO: validate UTF-8 chars
			outStream.write(Charset.forName("UTF-8").encode(msg).array());
			outStream.flush();
		}
	}
	
	
	/*
	 * Reads from server displaying messages and handles cmds that come from server
	 */
	private static class readFromServer implements Runnable
	{
		private final BufferedInputStream inStream;
		private final BufferedOutputStream outStream;
		
		readFromServer(BufferedInputStream inStream,BufferedOutputStream outStream)
		{
			this.inStream = inStream;
			this.outStream = outStream;
		}
		
		@Override
		public void run() {
			try {
				while(true)
				{
					byte[] data = new byte[MAXMESSAGEBYTES];
					inStream.read(data,0,MAXMESSAGEBYTES);//Reads 16bytes from stream	
					String msg = getStringFromRawData(data);
			
					//check if cmd
					if(msg.startsWith("/"))
					{
						System.out.println("\nServer: "+new String(msg));
						handleCommandFromServer(msg);
					}
					else
					{
						System.out.println("\n"+connectedToUser+": "+new String(msg));
						System.out.print("Enter cmd/msg: ");
					}
				}
			}catch (SocketException e){
				System.out.println("\nRead thread disconnected.");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		
		/*
		 * Handle actions associated with cmds the server may send user
		 */
		private void handleCommandFromServer(String cmd)
		{
			if(cmd.startsWith("/c"))// connect to user
			{
				//finds name surrounded by parenthesis
				Pattern p = Pattern.compile("(?:\"(?<name>.+)\"){1}?");
				Matcher m = p.matcher(new String(cmd));
				if(m.find())
				{
					incommingConnectionRequest = true;
					String name = m.group("name");			
					connectedToUser = name;
					
					//Display info to user
					System.out.printf("Incomming connection request from %s.\n",name);
					System.out.println("To Accept type exactly:/c \""+name+"\"");
					System.out.print("Enter cmd/msg (/h for help): ");
				}
			}
		}
		
		/*
		 *  removes zero entries from end of byte array return UTF-8 rep string
		 */
		public static String getStringFromRawData(byte[] data)
		{
			int numOfZeroBytesAtEnd = 0;
			for(int i=0;i<data.length;i++)
			{
				if(data[i] == 0)
				{
					numOfZeroBytesAtEnd++;
				}
				else
				{
					numOfZeroBytesAtEnd=0;
				}
			}
			int newSize = data.length-numOfZeroBytesAtEnd;//1-1=0 0,0
			byte[] newData = Arrays.copyOfRange(data, 0, newSize);
			String mess = Charset.forName("UTF-8").decode(ByteBuffer.wrap(newData)).toString();
			return mess;
		}
		
	}

	/*
	 * Gets input from console 
	 */
	static String getInputFromConsole(int minLength,int maxLength)
	{
		String input = null;
		if(consoleIn == null)
		{
			consoleIn = new Scanner(System.in);
		}
		boolean valid = true;
		do
		{
			input = consoleIn.nextLine();
			if( input.length() < minLength || input.length() > maxLength )
			{
				valid = false;
				System.out.println("Invalid ");
			}
			
		}while(!valid);
		return input;
	}

}