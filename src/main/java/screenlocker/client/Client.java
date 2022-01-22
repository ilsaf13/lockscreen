package screenlocker.client;

import screenlocker.Props;

import javax.net.ssl.SSLSocketFactory;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {
    PrintWriter out;
    BufferedReader in;
    String host;
    int port;
    String trustStore;
    int socketDelay;
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
        socketDelay = properties.getInt("socketDelay", 10000);
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
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            Socket socket = null;
            Thread keepalive = null;
            try {
                lockScreen.setInfoMessage("Connecting");
                socket = factory.createSocket();
                SocketAddress socketAddress = new InetSocketAddress(host, port);
                socket.connect(socketAddress, socketDelay);
                socket.setSoTimeout(2 * socketDelay);
                socket.setKeepAlive(true);

                String macString = getMacString(NetworkInterface.getByInetAddress(socket.getLocalAddress()).getHardwareAddress());
                String ipString = socket.getLocalAddress().getHostAddress();

                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream());

                System.out.println("Connected to server");

                if (macString != null) {
                    System.out.println("Sending MAC");
                    send("MAC " + macString);
                }
                if (ipString != null) {
                    System.out.println("Sending IP");
                    send("IP " + ipString);
                }

                //keep alive
                keepalive = new Thread(new SocketKeepAlive(this));
                keepalive.start();
                System.out.println("Started " + keepalive.getId());

                lockScreen.setInfoMessage("");
                String line;
                while ((line = in.readLine()) != null) {
//                    System.out.printf("Client got '%s'\n", line);
                    if (line.equals("exit")) {
                        exit();
                        return;
                    } else if (line.equals("stop alttab")) {
                        lockScreen.ats.stop();
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
                    } else if (line.equalsIgnoreCase("set lockscreenId")) {
                        if (id != null) lockScreen.setId(id);
                    } else if (line.equalsIgnoreCase("show id")) {
                        lockScreen.setInfoMessage("" + id);
                    } else if (line.equalsIgnoreCase("show ip")) {
                        lockScreen.setInfoMessage("" + socket.getLocalAddress().getHostAddress());
                    } else if (line.startsWith("show ")) {
                        lockScreen.setInfoMessage(line.substring("show ".length()));
                    } else if (line.equalsIgnoreCase("ping")) {
                        send("echo " + id);
                    } else if (line.equals("alive")) {
                        //ignore
                        System.out.println("Server is alive");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                lockScreen.setInfoMessage("OFFLINE");
                System.out.printf("Waiting %d seconds to reconnect\n", socketDelay / 1000);
                Thread.sleep(socketDelay);
            }

            try {
                if (keepalive != null) {
                    System.out.println("stopping keep alive thread");
                    keepalive.interrupt();
                }
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
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
