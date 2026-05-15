package com.example.master.net;

import com.example.master.service.MasterRequestHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Master 对客户端开放的 TCP 服务。
 *
 * <p>每个连接一个线程（线程池），按行收发。客户端发送一行请求，Master 回一行响应后即可关闭。
 * 这与 Client 模块 MasterClient 中的"短连接 + readLine"使用方式保持一致。
 */
public final class ClientSocketServer {

    private final int port;
    private final MasterRequestHandler handler;
    private final ExecutorService pool;
    private final AtomicInteger connSeq = new AtomicInteger();

    private volatile ServerSocket serverSocket;
    private volatile boolean running = false;

    public ClientSocketServer(int port, MasterRequestHandler handler) {
        this.port = port;
        this.handler = handler;
        this.pool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "master-client-conn-" + connSeq.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    /** 在调用线程内阻塞 accept。一般由独立线程启动。 */
    public void serve() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("Master client socket listening on :" + port);
        while (running) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                if (!running) {
                    break;
                }
                System.err.println("Accept failed: " + e.getMessage());
                continue;
            }
            pool.submit(new ClientConnectionHandler(socket, handler));
        }
    }

    public void shutdown() {
        running = false;
        ServerSocket s = serverSocket;
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }
        pool.shutdown();
    }
}
