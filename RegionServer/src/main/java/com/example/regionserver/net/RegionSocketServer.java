package com.example.regionserver.net;

import com.example.regionserver.service.RSRequestHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RegionServer TCP socket server, one request per connection.
 */
public final class RegionSocketServer {

    private final int port;
    private final RSRequestHandler handler;
    private final ExecutorService pool;
    private final AtomicInteger connSeq = new AtomicInteger();

    private volatile ServerSocket serverSocket;
    private volatile boolean running = false;

    public RegionSocketServer(int port, RSRequestHandler handler) {
        this.port = port;
        this.handler = handler;
        this.pool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "region-conn-" + connSeq.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    public void serve() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("RegionServer socket listening on :" + port);
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
            pool.submit(new RegionConnectionHandler(socket, handler));
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

