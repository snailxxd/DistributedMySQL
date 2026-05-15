package com.example.master.net;

import com.example.master.service.MasterRequestHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * 单个客户端连接的请求处理。
 *
 * <p>协议：客户端每次发一行（以 \n 结尾），Master 回一行。
 * 为兼容 MasterClient 在不同请求间复用/不复用连接两种用法，
 * 这里循环读取直到对端关闭。
 */
final class ClientConnectionHandler implements Runnable {

    private final Socket socket;
    private final MasterRequestHandler handler;

    ClientConnectionHandler(Socket socket, MasterRequestHandler handler) {
        this.socket = socket;
        this.handler = handler;
    }

    @Override
    public void run() {
        String remote = socket.getInetAddress() + ":" + socket.getPort();
        try (Socket s = socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String response = safeHandle(line);
                writer.write(response);
                writer.write('\n');
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("Client connection " + remote + " error: " + e.getMessage());
        }
    }

    private String safeHandle(String line) {
        try {
            return handler.handle(line);
        } catch (RuntimeException e) {
            return "ERR internal: " + e.getMessage();
        }
    }
}
