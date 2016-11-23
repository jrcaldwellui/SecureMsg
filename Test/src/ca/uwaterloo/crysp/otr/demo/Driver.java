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

package ca.uwaterloo.crysp.otr.demo;

/**
 * This class simulates two IM clients talking in OTR
 *
 * @author Can Tang <c24tang@gmail.com>
 */

import java.net.*;
import java.io.*;
import ca.uwaterloo.crysp.otr.*;

import ca.uwaterloo.crysp.otr.iface.*;

public class Driver {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws OTRException 
	 */
	public static void main(String[] args) throws IOException, OTRException {

		System.out.println("Starting driver...");
		if(args.length!=1){
			System.out.println("Please enter \"alice\" or \"bob\" as the argument");
			return;
		}
		if(args[0].equals("alice")){
			
			// building the connection
			ServerSocket server=new ServerSocket(53617);
			Socket client = server.accept();
			BufferedReader in=new BufferedReader(new InputStreamReader(client.getInputStream()));
			BufferedReader in2=new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Connected to Bob");
			
			// Generate the keys
			OTRInterface alice = new UserState(new ca.uwaterloo.crysp.otr.crypt.jca.JCAProvider());
			OTRCallbacks callback = new LocalCallback(client);
			
			// Send and receive the message repeatedly
			new SendingThread(in2, alice, "alice.msn.com", "msn", "bob@msn.com", callback).start();
			new ReceivingThread(in, alice, "alice.msn.com", "msn", "bob@msn.com",callback).start();
			
		}else if(args[0].equals("bob")){
			// building the connection
			Socket client=new Socket(
					InetAddress.getLocalHost(),
					53617);
			BufferedReader in=new BufferedReader(new InputStreamReader(client.getInputStream()));
			BufferedReader in2=new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Connected to Alice");
			
			// Generate the keys
			OTRInterface bob = new UserState(new ca.uwaterloo.crysp.otr.crypt.jca.JCAProvider());		
			OTRCallbacks callback = new LocalCallback(client);
	
			// Send and receive the message repeatedly
			new SendingThread(in2, bob, "bob.msn.com", "msn", "alice@msn.com", callback).start();
			new ReceivingThread(in, bob, "bob.msn.com", "msn", "alice@msn.com", callback).start();
			
		}else{
			System.out.println("Please enter \"alice\" or \"bob\" as the argument");
		}
		//System.out.println("");
	}

}

class SendingThread extends Thread{
	private BufferedReader in;
	private OTRContext conn;
	private OTRCallbacks callback;
	private OTRInterface us;
	private String accountname;
	private String protocol;
	private String recipient;
	
	public SendingThread(BufferedReader in, OTRInterface us, String accName,
			String prot, String recName, OTRCallbacks callbacks){
		this.in=in;
		this.us=us;
		this.accountname = accName;
		this.protocol = prot;
		this.recipient = recName;
		this.conn=us.getContext(accName, prot, recName);
		this.callback = callbacks;
	}
	
	public void run(){
		String str;
		while(true){
			try {
				str = in.readLine();
				if(str.startsWith("/isq")){
					System.out.println("Please input the question");
					String question = in.readLine();
					System.out.println("Please input the secret");
					str = in.readLine();
					conn.initiateSmp_q(question, str, callback);
				}else if(str.startsWith("/is")){
					System.out.println("Please input the secret");
					str = in.readLine();
					conn.initiateSmp(str, callback);
				}else if(str.startsWith("/rs")){
					System.out.println("Please input the secret");
					str = in.readLine();
					conn.respondSmp(str, callback);
				}else if(str.startsWith("/as")){
					conn.abortSmp(callback);
				}else if(str.startsWith("/disc")){
					conn.disconnect(callback);
				}
				else{
					System.out.println("To OTR:"+str.length()+":"+str);
					OTRTLV[] tlvs = new OTRTLV[1];
					tlvs[0]=new TLV(9, "TestTLV".getBytes());
					us.messageSending(accountname, protocol, recipient,
							str, tlvs, Policy.FRAGMENT_SEND_ALL, callback);
					/*if(str.length()!=0){
						System.out.println("To network:"+str.length()+":\033[35m"+str+"");
						conn.fragmentAndSend(str,  callback);
					}*/
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
	}
}

class ReceivingThread extends Thread{
	private BufferedReader in;
	private OTRInterface us;
	private String accountname;
	private String protocol;
	private String sender;
	private OTRContext conn;
	private OTRCallbacks callback;
	
	public ReceivingThread(BufferedReader in, OTRInterface us, String accName,
			String prot, String sendName, OTRCallbacks callbacks){
		this.in=in;
		this.us=us;
		this.accountname = accName;
		this.protocol = prot;
		this.sender = sendName;
		this.conn=us.getContext(accName, prot, sendName);
		this.callback = callbacks;
	}
	
	public void run(){
		String res;
		while(true){
			try {
				res=in.readLine();
				System.out.println("From network:"+res.length()+":"+res);
				StringTLV stlv = us.messageReceiving(accountname, protocol, sender, res, callback);
				if(stlv!=null){
					res=stlv.msg;
					System.out.println("From OTR:"+res.length()+":"+res);
				}
			} catch (SocketException e) {
				return;
			}
			catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
	}
}

class LocalCallback implements OTRCallbacks{
	
	Socket soc;
	PrintWriter out;
	
	public LocalCallback(Socket sock) throws IOException{
		soc=sock;
		out=new PrintWriter(soc.getOutputStream());
	}

	public void injectMessage(String accName, String prot, String rec, String msg){
		if(msg==null)return;
		System.out.println("Injecting message to the recipient:"
				+msg.length()+":"+msg);
		out.println(msg);
		out.flush();
	}

	public int getOtrPolicy(OTRContext conn) {
		return Policy.DEFAULT;
	}

	public void goneSecure(OTRContext context) {
		System.out.println("AKE succeeded");
	}

	public int isLoggedIn(String accountname, String protocol,
			String recipient) {
		return 1;
	}

	public int maxMessageSize(OTRContext context) {
		return 1000;
	}

	public void newFingerprint(OTRInterface us,
			String accountname, String protocol, String username,
			byte[] fingerprint) {
		System.out.println("New fingerprint is created.");
	}

	public void stillSecure(OTRContext context, int is_reply) {
		System.out.println("[31mStill secure.");
	}

	public void updateContextList() {
		System.out.println("Updating context list.");
	}

	public void writeFingerprints() {
		System.out.println("Writing fingerprints.");
	}

	public String errorMessage(OTRContext context, int err_code) {
		if(err_code==OTRCallbacks.OTRL_ERRCODE_MSG_NOT_IN_PRIVATE){
			return "You sent an encrypted message, but we finished" +
					"the private conversation.";
		}
		return null;
	}

	public void handleMsgEvent(int msg_event,
			OTRContext context, String message) {
		if(msg_event==OTRCallbacks.OTRL_MSGEVENT_CONNECTION_ENDED){
			System.out.println("The private connection has already ended.");
		}else if(msg_event==OTRCallbacks.OTRL_MSGEVENT_RCVDMSG_NOT_IN_PRIVATE){
			System.out.println("We received an encrypted message, but we are not in" +
					"encryption state.");
		}
	}

	public void handleSmpEvent(int smpEvent,
			OTRContext context, int progress_percent, String question) {
		if(smpEvent == OTRCallbacks.OTRL_SMPEVENT_ASK_FOR_SECRET){
			System.out.println("The other side has initialized SMP." +
					" Please respond with /rs.");
		}else if(smpEvent == OTRCallbacks.OTRL_SMPEVENT_ASK_FOR_ANSWER){
			System.out.println("The other side has initialized SMP, with question:" +
					question + ", "+
			" Please respond with /rs.");
		}else if(smpEvent == OTRCallbacks.OTRL_SMPEVENT_SUCCESS){
			System.out.println("SMP succeeded.");
		}else if(smpEvent == OTRCallbacks.OTRL_SMPEVENT_FAILURE){
			System.out.println("SMP failed.");
		}
		
		
	}
	
}
