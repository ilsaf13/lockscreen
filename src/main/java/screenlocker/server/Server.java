package screenlocker.server;

import screenlocker.Props;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.ServerSocket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    static volatile long unlockTime = -1;
    //pin code must be 5 digits
    static String pinCode = "12345";
    static final Map<Long, ServerThread> threads = new ConcurrentHashMap<>();
    //maps clients MAC to unique ID
    static Map<String, String> clientIds;
    static Props properties;

    public static void main(String[] args) throws IOException {

        properties = new Props(args[0]);

        System.setProperty("javax.net.ssl.keyStore", properties.get("keyStore"));
        System.setProperty("javax.net.ssl.keyStorePassword", properties.get("keyStorePassword"));
        SSLServerSocketFactory socketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        ServerSocket serverSocket = socketFactory.createServerSocket(properties.getInt("port"));

        loadClientIds();

        new Thread() {
            @Override
            public void run() {
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

                while (true) {
                    try {
                        String line = br.readLine();
                        if (line.startsWith("set unlockTime ")) {
                            unlockTime = -1;
                            try {
                                String t = line.substring("set unlockTime ".length());
                                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                                long day = 24 * 60 * 60 * 1000;
                                unlockTime = sdf.parse(t).getTime() + System.currentTimeMillis() / day * day;
                                System.out.println(new Date(unlockTime));
                            } catch (ParseException ignored) {
                            }
                            line = "set unlockTime " + unlockTime;

                        } else if (line.equals("set serverTime")) {
                            line += " " + System.currentTimeMillis();
                        } else if (line.startsWith("set pinCode ")) {
                            String tmp = line.substring("set pinCode ".length());
                            if (tmp.length() != 5) {
                                System.out.println("PIN code must be 5 chars!");
                                continue;
                            }
                            synchronized (pinCode) {
                                pinCode = tmp;
                            }
                        } else if (line.equals("save")) {
                            saveClientIds();
                            continue;
                        }
                        int cnt = 0;
                        for (ServerThread t : threads.values()) {
                            t.send(line);
                            cnt++;
                        }
                        System.out.printf("Sent to %d clients '%s'\n", cnt, line);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        while (true) {
            ServerThread st = new ServerThread(serverSocket.accept());
            threads.put(st.getId(), st);
            st.start();
        }
    }

    static void loadClientIds() {
        Props p;
        try {
            p = new Props(properties.get("clientIds"));
            clientIds = p.toConcurrentHashMap();
        } catch (IOException ignored) {
            System.out.printf("ERROR: Couldn't load client IDs from '%s'\n", properties.get("clientIds"));
        }
    }

    static void saveClientIds() {
        try {
            new Props(clientIds).store(new FileWriter(properties.get("clientIds")));
            System.out.println(properties.get("clientIds") + " saved");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
