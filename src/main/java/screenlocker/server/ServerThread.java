package screenlocker.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerThread extends Thread {
    Socket socket;
    final PrintWriter out;
    BufferedReader in;
    Map<String, String> params;
    private BlockingQueue<String> msgQueue = new LinkedBlockingQueue<>();
    Thread printThread;
    static final boolean log = false;

    public ServerThread(Socket socket) throws IOException {
        this.socket = socket;
        out = new PrintWriter(socket.getOutputStream());
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        printThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    out.println(msgQueue.take());
                    out.flush();
                }
            } catch (InterruptedException e) {
                //Thread.currentThread().interrupt();
            }
            if (log) System.out.println("Stopping thread " + Thread.currentThread().getId());
        });
        printThread.start();
    }

    public void run() {
        try {
            params = new HashMap<>();
            if (Server.unlockTime > 0) {
                send("set unlockTime " + Server.unlockTime);
            }
            if (Server.pinCode != null) {
                send("set pinCode " + Server.pinCode);
            }
            socket.setSoTimeout(Server.socketSoTimeout);
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("exit")) break;
                else if (line.startsWith("MAC ")) {
                    String mac = line.substring("MAC ".length());
                    String id = Server.clientIds.computeIfAbsent(mac, k -> "#" + this.getId());
                    send("set id " + id);
                    params.put("MAC", mac);
                    params.put("ID", id);
                    if (log) System.out.printf("Client %s connected\n", id);
                } else if (line.startsWith("IP ")) {
                    String ip = line.substring("IP ".length());
                    params.put("IP", ip);
                    Server.clientIps.put(ip, params.getOrDefault("ID", "NULL"));
                } else if (line.startsWith("echo ")) {
                    System.out.printf("Got echo %d times\r", Server.pingSuccess.incrementAndGet());
                } else if (line.equals("keepalive")) {
                    if (log) System.out.println("Keepalive from " + params.get("ID"));
                    send("alive");
                } else {
                    System.out.printf("Thread %d got '%s'\n", getId(), line);
                }
            }
            if (log) System.out.printf("Thread %d exited\n", getId());

        } catch (IOException e) {
            if (log) e.printStackTrace();
        }
        try {
            printThread.interrupt();
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (log) System.out.printf("Thread %d exited\n", getId());
        Server.threads.remove(getId());
    }

    public void send(String s) {
        msgQueue.offer(s);
    }
}
