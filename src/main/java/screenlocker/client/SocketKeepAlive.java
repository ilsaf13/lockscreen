package screenlocker.client;

public class SocketKeepAlive implements Runnable {
    Client parent;

    public SocketKeepAlive(Client client) {
        parent = client;
    }

    @Override
    public void run() {
        System.out.println("Starting keep alive thread");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                parent.send("keepalive");
                Thread.sleep(parent.socketDelay);
//            System.out.println("Running " + Thread.currentThread().getId());
            }
        } catch (InterruptedException e) {
            //e.printStackTrace();
            //Thread.currentThread().interrupt();
        }
        System.out.println("Stopped keep alive thread");
    }
}
