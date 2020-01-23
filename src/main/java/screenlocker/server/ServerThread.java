package screenlocker.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ServerThread extends Thread {
    Socket socket;
    PrintWriter out;
    BufferedReader in;
    Map<String, String> params;

    public ServerThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            params = new HashMap<>();

            if (Server.unlockTime > 0) {
                send("set unlockTime " + Server.unlockTime);
            }
            if (Server.pinCode != null) {
                send("set pinCode " + Server.pinCode);
            }

            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("exit")) break;
                else if (line.startsWith("MAC ")) {
                    String mac = line.substring("MAC ".length());
                    String id = Server.clientIds.computeIfAbsent(mac, k -> "#" + this.getId());
                    send("set id " + id);
                    params.put("MAC", mac);
                    params.put("ID", id);
                    System.out.printf("Client %s connected\n", id);
                } else if (line.startsWith("IP ")) {
                    String ip = line.substring("IP ".length());
                    params.put("IP", ip);
                    Server.clientIps.put(ip, params.getOrDefault("ID", "NULL"));
                } else if (line.startsWith("echo ")) {
                    System.out.printf("Got echo %d times\r", Server.pingSuccess.incrementAndGet());
                } else {
                    System.out.printf("Thread %d got '%s'\n", getId(), line);
                }
            }
            System.out.printf("Thread %d exited\n", getId());
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Server.threads.remove(getId());
    }

    public void send(String s) {
        out.println(s);
        out.flush();
    }
}
