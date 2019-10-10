package screenlocker.client;

import screenlocker.Props;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;

public class LockScreen implements Runnable {
    private String defaultFont = "Times New Roman Plain 100";
    private float margin = 0.03f;
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    private final Boolean sync = false;

    Client client;
    Frame mainFrame;
    AltTabStopper ats;
    GraphicsDevice device;
    BufferedImage image;
    Props props;
    volatile long unlockTime;
    volatile long timeDelay;
    volatile boolean running = true;
    final int[] pinCode = new int[]{KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5};
    ArrayDeque<Integer> keysPressed = new ArrayDeque<>();
    String countdownTemplate;

    Photo photo;
    BufferedImage bgImg;
    StaticText id;
    StaticText name;
    StaticText message;
    StaticText infoMessage;
    StaticText time;
    StaticText os;

    Color bgColor = Color.blue;

    private static DisplayMode[] BEST_DISPLAY_MODES;

    public void init(Props props) {
        this.props = props;

        try {
            bgImg = Util.loadJpegImage(props.getString("backgroundImg", ""));
        } catch (Exception e) {
            System.out.println("Error loading background image");
        }
        try {
            photo = new Photo(props.getString("photoFileName", ""),
                    props.getFraction("photoHorz", "0"),
                    props.getFraction("photoVert", "0")
            );
        } catch (IOException e) {
            System.out.println("Error loading photo");
        }
        id = getStaticText(props, "id",
                defaultFont, Color.white,
                "100%", "0%", margin);
        name = getStaticText(props, "name",
                defaultFont, Color.white,
                "100%", "15%", margin);
        message = getStaticText(props, "message",
                defaultFont, Color.white,
                "50%", "60%", margin);
        time = getStaticText(props, "time",
                defaultFont, Color.white,
                "50%", "75%", margin);
        infoMessage = getStaticText(props, "infoMessage",
                defaultFont, Color.yellow,
                "50%", "40%", margin);
        os = getStaticText(props, "os",
                defaultFont, Color.cyan,
                "0%", "100%", margin);
        os.setText(System.getProperty("os.name"));
        countdownTemplate = props.getString("countdownTemplate", "Contest starting in %s");
    }

    public LockScreen(Client client, Props props) {
        this.client = client;

        String[] dmodes = props.getString("displayModes",
                "640x480 32 bit, 640x480 16 bit, 640x480 8 bit")
                .split(",");
        BEST_DISPLAY_MODES = new DisplayMode[dmodes.length];
        for (int i = 0; i < dmodes.length; i++) {
            BEST_DISPLAY_MODES[i] = getDisplayMode(dmodes[i]);
        }

        GraphicsEnvironment env = GraphicsEnvironment.
                getLocalGraphicsEnvironment();
        device = env.getDefaultScreenDevice();
        GraphicsConfiguration gc = device.getDefaultConfiguration();
        mainFrame = new Frame(gc);
        ats = new AltTabStopper(mainFrame);
        mainFrame.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ALT) return;
                if (e.getKeyCode() == KeyEvent.VK_WINDOWS) {
                    System.out.println("Windows key pressed");
                }
                keysPressed.addLast(e.getKeyCode());
                if (checkPinCode()) {
                    ats.stop();
                }
            }
        });
        mainFrame.setUndecorated(true);
        mainFrame.setIgnoreRepaint(true);
        mainFrame.setAlwaysOnTop(true);

        device.setFullScreenWindow(mainFrame);
        if (device.isDisplayChangeSupported()) {
            chooseBestDisplayMode();
        }
        init(props);
        updateImage();
    }

    public void run() {
        int numBuffers = 2;
        Rectangle bounds = mainFrame.getBounds();
        mainFrame.createBufferStrategy(numBuffers);
        BufferStrategy bufferStrategy = mainFrame.getBufferStrategy();
        new Thread(ats).start();
        while (unlockTime <= 0 || unlockTime > System.currentTimeMillis() + timeDelay) {
            Graphics2D g = (Graphics2D) bufferStrategy.getDrawGraphics();
            g.setRenderingHints(Util.HINTS);
            if (!bufferStrategy.contentsLost()) {
                Font font = time.getBaseFont();
                g.drawImage(image, 0, 0, bounds.width, bounds.height, mainFrame);
                g.setFont(font);

                String line;
                if (unlockTime <= 0) {
                    line = sdf.format(System.currentTimeMillis() + timeDelay);
//                    g.drawString(sdf.format(System.currentTimeMillis() + timeDelay), 300, 800);
                } else {
                    line = getCountdownMsg();
//                    g.drawString(getCountdownMsg(), 300, 800);
                }
                int width = g.getFontMetrics(font).stringWidth(line);
                int height = g.getFontMetrics(font).getHeight();

                int x = (int) ((bounds.width - width) * time.getHorizontalPosition());
                int y = (int) ((bounds.height - height) * time.getVerticalPosition()) + g.getFontMetrics().getAscent();
                g.setColor(Color.BLACK);
                g.drawString(line, x, y + StaticText.SHADOW_OFFSET);
                g.setColor(time.getColor());
                g.drawString(line, x, y);
                bufferStrategy.show();
                g.dispose();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        ats.stop();
        mainFrame.dispose();
        running = false;
        System.out.println("Stopping lock screen");
        client.exit();
    }


    void updateImage() {
        int width = device.getDisplayMode().getWidth();
        int height = device.getDisplayMode().getHeight();
        BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = res.createGraphics();
        g.setColor(bgColor);
        g.fillRect(0, 0, width, height);
        g.drawImage(bgImg, 0, 0, width, height, mainFrame);
        if (photo != null) {
            int x = (int) ((width - photo.width) * photo.horz);
            int y = (int) ((height - photo.height) * photo.vert);
            g.drawImage(photo.img, x, y, photo.width, photo.height, mainFrame);
        }
        g.dispose();
        if (os != null) {
            res = os.modify(res);
        }
        if (id != null) {
            res = id.modify(res);
        }
        if (name != null) {
            res = name.modify(res);
        }
        if (message != null) {
            res = message.modify(res);
        }
        if (infoMessage != null) {
            res = infoMessage.modify(res);
        }


        image = res;
    }

    private StaticText getStaticText(Props props, String key, String defaultFont,
                                     Color defaultColor, String defaultHorz,
                                     String defaultVert, float margin) {
        return new StaticText(
                props.getString(key, ""),
                props.getFont(key + "Font", defaultFont),
                props.getColor(key + "Color", defaultColor),
                props.getFraction(key + "Horz", defaultHorz),
                props.getFraction(key + "Vert", defaultVert),
                margin
        );
    }

    public void chooseBestDisplayMode() {
        DisplayMode best = getBestDisplayMode();
        if (best != null) {
            device.setDisplayMode(best);
        }
    }

    private DisplayMode getBestDisplayMode() {
        for (int x = 0; x < BEST_DISPLAY_MODES.length; x++) {
            if (BEST_DISPLAY_MODES[x] == null) continue;
            DisplayMode[] modes = device.getDisplayModes();
            for (int i = 0; i < modes.length; i++) {
                if (modes[i].getWidth() == BEST_DISPLAY_MODES[x].getWidth()
                        && modes[i].getHeight() == BEST_DISPLAY_MODES[x].getHeight()
                        && modes[i].getBitDepth() == BEST_DISPLAY_MODES[x].getBitDepth()
                ) {
                    return BEST_DISPLAY_MODES[x];
                }
            }
        }
        return null;
    }

    private DisplayMode getDisplayMode(String s) {
        s = s.trim();
        if (s.matches("\\d+x\\d+ \\d+ bit")){
            String[] parts = s.split("[x ]");
            int width = Integer.parseInt(parts[0]);
            int height = Integer.parseInt(parts[1]);
            int bit = Integer.parseInt(parts[2]);
            return new DisplayMode(width, height, bit, 0);
        } else
            return null;
    }

    String getCountdownMsg() {
        long left = unlockTime - (System.currentTimeMillis() + timeDelay);
        long s = left / 1000 % 60;
        long m = left / 1000 / 60 % 60;
        long h = left / 1000 / 60 / 60;
        String time;
        if (h > 0) {
            time = String.format("%d:%02d:%02d", h, m, s);
        } else {
            time = String.format("%d:%02d", m, s);
        }
        return String.format(countdownTemplate, time);
    }

    boolean checkPinCode() {

        if (keysPressed.size() >= pinCode.length) {
            System.out.println("Checking PIN");
            boolean equals = true;
            Integer[] keys = keysPressed.toArray(new Integer[0]);
            for (int i = 0; i < pinCode.length; i++) {
                if (pinCode[i] != keys[i]) {
                    equals = false;
                    break;
                }
            }
            keysPressed.removeFirst();
            return equals;
        }
        return false;
    }

    void setInfoMessage(String st) {
        infoMessage.setText(st);
        updateImage();
    }

    void setId(String st) {
        id.setText(st);
        updateImage();
    }

    class Photo {

        String fileName;
        float horz, vert;
        int width, height;

        BufferedImage img;

        public Photo(String fileName, float horz, float vert) throws IOException {
            this.fileName = fileName;
            this.horz = horz;
            this.vert = vert;
            img = Util.loadJpegImage(fileName);
            width = img.getWidth();
            height = img.getHeight();
        }
    }
}
