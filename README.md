To test OTR:   
open /Test proj in eclipse  
run /Test/src/ca/uwaterloo/crysp/otr/demo/Driver.java with command line arg alice, then run again with argument bob  

Current functionallity:

Multiple clients can connect to the server  
Each can start a chat session with one other user  

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
type anything then press enter, starts key exchange with server  

start client  
type in username then press enter  
type anything then press enter, starts key exchange with server    
  
type /c "firstuser" on one client to start session with firstuser   
type /c "seconduser" on other client to confirm session  with second user  

should get session confimation on both clients    
type anything thing then press enter on a client, this starts key exchange   
should be able to chat now  
note: cmds are broken is session, /d wont work  

test smp:    
while in session  
one client types /isq  
client should be prompted for secret question and answer    
session partner will be prompted for response with  /rs   
both users will see if it passed   





