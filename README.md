To test OTR:   
open /Test proj in eclipse  
run /Test/src/ca/uwaterloo/crysp/otr/demo/Driver.java with command line arg alice, then run again with argument bob  

Current functionallity:

Multiple clients can connect to the server  
Each can start a chat session with one other user  
OTR protocol with server and client  
OTR protocol between clients   
Basic SMP between clients   


client cmds     | function  
/h              | help, list cmds  
/c "username"   | start chat session with username(type qoutes)  
/d              | leave chat session      
/e              | disconect from server  
  
TODO:  
Make functionallity cleaner  
Look into removing metadata?  
Cmds break once session is started  
 
TESTING:  
start server

start client  
type in username then press enter  

start client  
type in username then press enter  
  
type /c "firstusername" on one client to start session with someone named firstusername    
type /c "secondusername" on other client to confirm session  with someone named secondusername    

should get session confimation on both clients    
type anything thing then press enter on a client, this starts key exchange   
should be able to chat now  

test smp:    
while in session  
one client types /isq  
client should be prompted for secret question and answer    
session partner will be prompted for response with  /rs   
both users will see if it passed   





