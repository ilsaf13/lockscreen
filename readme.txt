Supported commands:
   	set unlockTime <HH:mm> - sets unlock time on clients
   	set serverTime - sends server time in milliseconds to clients to synchronize unlock time. Does not take into account network latency!
   	set pinCode <5 chars pin code> - sets pin code on clients used to stop alt+tab stopper

   	set lockscreenId - shows client ID instead of computer ID
   	save - saves client IDs into .properties file
   	exit - stops all clients
   	reload - reloads all clients

   	show id - shows client ID in information message
   	show ip - shows client IP in information message

   	MAC <00:00:00:00:00> - sent by client when the socket is established. Server sets client ID according to MAC
   	set id <id> - server automatically sets unique ID for each client when socket is established using this command