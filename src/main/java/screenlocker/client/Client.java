package screenlocker.client;

import screenlocker.Props;

import javax.net.ssl.SSLSocketFactory;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Properties;

public class Client {
    PrintWriter out;
    String host;
    int port;
    String trustStore;
    long socketDelay;
    Props properties;
    String propertiesFileName;
    String id;

    public static void main(String[] args) throws InterruptedException, IOException {
        Client client = new Client();
        client.init(args[0]);
        client.run();
    }

    void init(String propertiesFileName) throws IOException {
        this.propertiesFileName = propertiesFileName;

        properties = new Props(propertiesFileName);
        host = properties.get("host");
        port = properties.getInt("port");
        trustStore = properties.get("trustStore");
        socketDelay = properties.getLong("socketDelay", 10000);
    }

    static String getMacString(byte[] mac) {
        StringBuilder macString = null;
        if (mac != null && mac.length > 0) {
            macString = new StringBuilder().append(String.format("%02X", mac[0]));
            for (int i = 1; i < mac.length; i++) {
                macString.append(":").append(String.format("%02X", mac[i]));
            }
        }
        if (macString != null) return macString.toString();
        return null;
    }

    void run() throws InterruptedException {
        LockScreen lockScreen = new LockScreen(this, properties);
        new Thread(lockScreen).start();
        System.setProperty("javax.net.ssl.trustStore", trustStore);
        while (lockScreen.running) {
            try {
                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                Socket socket = factory.createSocket(host, port);

                String macString = getMacString(NetworkInterface.getByInetAddress(socket.getLocalAddress()).getHardwareAddress());
                String ipString = socket.getLocalAddress().getHostAddress();

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream());
                System.out.println("Connected to server");

                if (macString != null) {
                    send("MAC " + macString);
                }
                if (ipString != null) {
                    send("IP " + ipString);
                }

                lockScreen.setInfoMessage("");
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.printf("Client got '%s'\n", line);
                    if (line.equals("exit")) {
                        exit();
                        return;
                    } else if (line.equals("reload")) {
                        init(propertiesFileName);
                        lockScreen.init(properties);
                        lockScreen.updateImage();
                        send("exit");
                        socket.close();
                        break;
                    } else if (line.startsWith("set unlockTime ")) {
                        lockScreen.unlockTime = Long.parseLong(line.substring("set unlockTime ".length()));
                    } else if (line.startsWith("set serverTime ")) {
                        lockScreen.timeDelay = Long.parseLong(line.substring("set serverTime ".length())) - System.currentTimeMillis();
                    } else if (line.startsWith("set pinCode ")) {
                        String pin = line.substring("set pinCode ".length());
                        synchronized (lockScreen.pinCode) {
                            for (int i = 0; i < lockScreen.pinCode.length; i++) {
                                lockScreen.pinCode[i] = KeyEvent.getExtendedKeyCodeForChar(pin.charAt(i));
                            }
                        }
                    } else if (line.startsWith("set id ")) {
                        id = line.substring("set id ".length());
                    } else if (line.equals("set lockscreenId")) {
                        if (id != null) lockScreen.setId(id);
                    } else if (line.equals("show id")) {
                        lockScreen.setInfoMessage("" + id);
                    } else if (line.equals("show ip")) {
                        lockScreen.setInfoMessage("" + socket.getLocalAddress().getHostAddress());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                lockScreen.setInfoMessage("OFFLINE");
                System.out.printf("Waiting %d seconds to reconnect\n", socketDelay / 1000);
                Thread.sleep(socketDelay);
            }
        }
    }

    void exit() {
        send("exit");
        System.exit(0);
    }

    void send(String line) {
        out.println(line);
        out.flush();
    }
}
