Supported commands:
   	set unlockTime <HH:mm> - sets unlock time on clients. Set unlock time to -1 makes it undefined again.
   	set serverTime - sends server time in milliseconds to clients to synchronize unlock time. Does not take into account network latency!
   	set pinCode <5 chars pin code> - sets pin code on clients used to stop alt+tab stopper

   	set lockscreenId - shows client ID instead of computer ID
   	save - saves client IDs into .properties file
   	save <key1> <key2> ... - saves client property with keys <key1>, <key2> and so on into client-<key1>-<key2>-<...>.properties file. Possible values for key: MAC, ID, IP.
   	exit - stops all clients
   	exit <id1> <id2> ... - stops listed clients
   	reload - reloads all clients

   	show id - shows client ID in information message
   	show ip - shows client IP in information message

   	MAC <00:00:00:00:00> - sent by client when the socket is established. Server sets client ID according to MAC
   	set id <id> - server automatically sets unique ID for each client when socket is established using this command
   	IP <10.10.0.1> - sent by client when the socket is established.

Ubuntu 18.04
    Edit the accessibility.properties file for OpenJDK:
        /etc/java-8-openjdk/accessibility.properties
    Comment out the following line:
        assistive_technologies=org.GNOME.Accessibility.AtkWrapper