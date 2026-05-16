package RegionManagers.SocketManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/** 多线程接受 Client 连接。 */
public class ClientSocketManager implements Runnable {

    private static final long ACCEPT_POLL_MS = 1000L;

    private final ServerSocket listenSocket;
    private final MasterSocketManager masterLink;
    private final Map<Socket, Thread> workerBySocket;

    public ClientSocketManager(int port, MasterSocketManager masterLink)
            throws IOException, InterruptedException {
        this.listenSocket = new ServerSocket(port);
        this.masterLink = masterLink;
        this.workerBySocket = new HashMap<>();
    }

    public void listenClient() throws InterruptedException, IOException {
        // 保留原接口，便于兼容旧调用
    }

    @Override
    public void run() {
        try {
            pollAcceptLoop();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    private void pollAcceptLoop() throws IOException, InterruptedException {
        for (;;) {
            Thread.sleep(ACCEPT_POLL_MS);
            Socket incoming = listenSocket.accept();
            attachWorker(incoming);
        }
    }

    private void attachWorker(Socket incoming) throws IOException {
        ClientThread session = new ClientThread(incoming, masterLink);
        Thread worker = new Thread(session);
        workerBySocket.put(incoming, worker);
        worker.start();
    }
}
