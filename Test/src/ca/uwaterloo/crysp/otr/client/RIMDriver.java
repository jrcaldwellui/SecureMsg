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

package ca.uwaterloo.crysp.otr.client;

/**
 * This class simulates two IM clients talking in OTR
 *
 * @author Can Tang <c24tang@gmail.com>
 */
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.system.*;
import ca.uwaterloo.crysp.otr.message.*;
import ca.uwaterloo.crysp.otr.iface.*;
import ca.uwaterloo.crysp.otr.*;
import ca.uwaterloo.crysp.otr.crypt.rim.RIMIO;
import javax.microedition.io.*;
import java.io.*;


public class RIMDriver extends UiApplication {

    private OTRScreen ms;

    public static void main(String[] args) throws Exception {
        RIMDriver driver = new RIMDriver();
        driver.enterEventDispatcher();
    }

    public RIMDriver() throws Exception {
        ms = new OTRScreen();
        ms.setTitle(new LabelField("OTR Driver Test"));
        pushScreen(ms);
    }
}

final class OTRScreen extends MainScreen {
    // declare variables for later use
    BasicEditField editField;
    private int select;
    ButtonField bt = new ButtonField("Connect", ButtonField.CONSUME_CLICK | ButtonField.NON_FOCUSABLE);
    RichTextField rt = new RichTextField("");
    OTRInterface bob;
    OTRContext b2a;
    InputStream is;
    OutputStream os;
    OTRCallbacks callback;
    boolean connected = false;
    PersistentObject store;

    public OTRScreen() throws Exception {
        super();
        LabelField title = new LabelField("OTR Demo", LabelField.ELLIPSIS
                | LabelField.USE_ALL_WIDTH);
        setTitle(title);
        store = PersistentStore.getPersistentObject(0x13245L);
        if (store.getContents() == null) {
            store.setContents("192.168.89.128:3333");
            store.commit();
        }
        String initial_add = (String)store.getContents();
        editField = new BasicEditField("Enter ip address and port:", initial_add);
        add(editField);

        ButtonListener bl = new ButtonListener(this);
        bt.setChangeListener(bl);
        add(bt);
        add(rt);

    }

    class ButtonListener implements FieldChangeListener {

        private OTRScreen otrs;

        public ButtonListener(OTRScreen otrs) {
            this.otrs = otrs;
        }

        public void fieldChanged(Field field, int context) {
            
            if(otrs.connected==false){
            StreamConnection conn;
            try {
                store = PersistentStore.getPersistentObject(0x13245L);
                store.setContents(otrs.editField.getText());
                store.commit();
                String address = "socket://"+otrs.editField.getText()+";deviceside=true";
                System.out.println("Address: "+address);
                conn = (StreamConnection) Connector
                        .open(address);
            } catch (Exception e) {
                System.out.println("Failed to connect to Alice.");
                return;
            }
            System.out.println("Connected to Alice");
            try{
                otrs.is = conn.openInputStream();
                otrs.os = conn.openOutputStream();
            }catch(Exception e){
                 System.out.println("Failed to get IOStream.");
                return;
            }
    

            try{
                otrs.callback = new LocalCallback(otrs.os);
            }catch(Exception e){
                 System.out.println("Failed to initiate callback.");
                return;
            }
            otrs.bob = new UserState(new ca.uwaterloo.crysp.otr.crypt.rim.RIMProvider());
            otrs.b2a = otrs.bob.getContext("bob@msn.com", "msn", "alice@msn.com");
            
            otrs.connected=true;
            new ReceivingThread(otrs).start();
            otrs.editField.setText("");
            otrs.bt.setLabel("Send");
            otrs.editField.setLabel("Enter your message:");
            return;
        }
            
            
            
            String str = otrs.editField.getText();
            otrs.editField.setText("");
            otrs.rt.setText(otrs.rt.getText() + "Bob:" + str + "\n");
            if(str.startsWith("/isq")){
                String question = str.substring(5, 8);
                str = str.substring(9,str.length());
                try{
                   b2a.initiateSmp_q(question,str, callback);
                }catch(Exception e){}
            }else if(str.startsWith("/is")){
                str = str.substring(4, str.length());
                try{
                   b2a.initiateSmp(str, callback);
                }catch(Exception e){}
            }else if(str.startsWith("/rs")){
                str = str.substring(4, str.length());
                try{
                   b2a.respondSmp(str, callback);
                }catch(Exception e){}
            }else{
                try {
                    str = otrs.bob.messageSending("bob@msn.com", "msn", "alice@msn.com",
                    str, null, Policy.FRAGMENT_SEND_ALL, callback);
                } catch (Exception e) {
                    System.out.println("Message sending failed.");
                    return;
                }
                try {
                    RIMIO.writeLine(os, str.getBytes());
                } catch (Exception e) {
                    System.out.println("writeLine failed.");
                }
            }
        }
    }
}

class ReceivingThread extends Thread implements Runnable{
    private InputStream in;
    private OTRContext conn;
    private OTRCallbacks callback;
    private OTRScreen otrs;
    private OTRInterface us;

    public ReceivingThread(OTRScreen otrs) {
        this.in = otrs.is;
        this.conn = otrs.b2a;
        this.callback = otrs.callback;
        this.us = otrs.bob;
        this.otrs = otrs;
    }

    public void run() {
        String res;
        while (true) {
            try {
                res = new String(RIMIO.readLine(in));
                System.out.println("From network:" + res.length() + ":" + res);
                StringTLV stlv = us.messageReceiving("bob@msn.com", "msn", "alice@msn.com",
                    res, callback); 
                if (stlv != null) {
                        final String res2 = stlv.msg;
                        if(res2.length()!=0){
                            UiApplication.getApplication().invokeLater(new Runnable(){
                                public void run(){
                                    otrs.rt.setText(otrs.rt.getText()+"Alice:"+res2+"\n");
                                }});
                            }
                            System.out.println("From otr:" +res2.length()+":"+ res2);
                }
            } catch (Exception e) {
                return;
            }
        }
    }
}

class LocalCallback implements OTRCallbacks {

    public OutputStream out;

    public LocalCallback(OutputStream out) throws IOException {
        this.out = out;
    }

    public int getOtrPolicy(OTRContext conn) {
        return Policy.DEFAULT;
    }

    public void injectMessage(String accName, String prot, String rec,
            String msg) {
        System.out.print("Injecting message to the recipient:" + msg.length());
        System.out.println(msg);
        try {
            RIMIO.writeLine(out, msg.getBytes());
        } catch (Exception e) {
            System.out.println("Failed to inject message.");
            return;
        }

    }

    public void authSucceeded() {
        System.out.println("AKE succeeded");
    }
    
    public int maxMessageSize(OTRContext conn) {
      return 3000;
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
       }
       else if(smpEvent == OTRCallbacks.OTRL_SMPEVENT_SUCCESS){
          System.out.println("SMP succeeded.");
       }else if(smpEvent == OTRCallbacks.OTRL_SMPEVENT_FAILURE){
          System.out.println("SMP failed.");
      }
      
       
   }
    
   public void createPrivkey(String accountname, String protocol) {
        
   }


 public void goneSecure(OTRContext context) {
        System.out.println("AKE succeeded");
 }

 public int isLoggedIn(String accountname, String protocol,
          String recipient) {
        return 0;
  }


 public void newFingerprint(OTRInterface us,
         String accountname, String protocol, String username,
          byte[] fingerprint) {
      
   }

 public void stillSecure(OTRContext context, int is_reply) {
     
   }

 public void updateContextList() {
     
   }

 public void writeFingerprints() {
     
   }
   

 public String errorMessage(OTRContext context, int err_code) {
      return null;
   }

 public void handleMsgEvent(int msg_event,
           OTRContext context, String message) {
      
   }


}
