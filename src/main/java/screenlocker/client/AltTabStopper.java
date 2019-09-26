package screenlocker.client;

import java.awt.*;
import java.awt.event.KeyEvent;

public class AltTabStopper implements Runnable {
    private volatile boolean working = true;
    private Frame frame;

    public AltTabStopper(Frame frame) {
        this.frame = frame;
    }

    public void stop() {
        working = false;
    }

    public static AltTabStopper create(Frame frame) {
        AltTabStopper stopper = new AltTabStopper(frame);
        new Thread(stopper, "Alt-Tab Stopper").start();
        return stopper;
    }

    public void run() {
        try {
            Robot robot = new Robot();
            while (working) {
                robot.keyRelease(KeyEvent.VK_ALT);
//                robot.keyRelease(KeyEvent.VK_TAB);
//                robot.keyRelease(KeyEvent.VK_DELETE);
//                robot.keyRelease(KeyEvent.VK_CONTROL);
//                robot.keyRelease(KeyEvent.VK_WINDOWS);
                frame.toFront();
                frame.requestFocus();
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        System.out.println("Stopping alt+tab stopper");
    }
}