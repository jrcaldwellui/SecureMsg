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
OTR encrypt comms between user and server    
OTR encrypt comms between user and user   
